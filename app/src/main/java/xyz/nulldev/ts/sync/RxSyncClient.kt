package xyz.nulldev.ts.sync

import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rx.Observable
import rx.schedulers.Schedulers
import xyz.nulldev.ts.sync.model.GetSyncStatusApiResponse
import xyz.nulldev.ts.sync.model.StartSyncApiResponse
import xyz.nulldev.ts.sync.model.SyncRequest
import xyz.nulldev.ts.sync.model.SyncResult
import java.util.*

class RxSyncClient(val endpoint: String,
                   val favoritesOnly: Boolean = false) {

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
                .baseUrl(endpoint)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    val service: SyncService by lazy {
        retrofit.create(SyncService::class.java)
    }

    /**
     * Sync libraries asynchronously
     * @param oldLibrary             The old library
     * @param newLibrary             The new library
     */
    fun syncLibrariesWithProgress(
            oldLibrary: String,
            newLibrary: String): Observable<SyncResult> {
        return trySync(oldLibrary, newLibrary).concatMap { taskId ->
            Observable.create<SyncResult> { sub ->
                while(true) {
                    val response = service.getSyncStatus(taskId).execute()
                    val taskStatus = response.body()
                    if(response.isSuccessful && taskStatus != null) {
                        val taskDetails = getTaskDetails(taskStatus)
                        val statusString = taskDetails.getString("status")
                        if (isSyncComplete(taskStatus, taskDetails)) {
                            //Declare we are complete and stop
                            sub.onNext(taskDetailsToSyncResult(taskDetails))
                            sub.onCompleted()
                            break
                        } else {
                            sub.onNext(SyncResult.Progress(statusString))
                        }
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Unknown error!"
                        sub.onNext(SyncResult.Fail(errorMessage))
                        sub.onError(RuntimeException(errorMessage))
                        break
                    }
                }
            }.subscribeOn(Schedulers.io())
        }
    }

    /**
     * Start a sync

     * @param oldLibrary The old library
     * *
     * @param newLibrary The new library
     * *
     * @return The id of the new sync task
     */
    private fun trySync(oldLibrary: String, newLibrary: String): Observable<Long> {
        val request = SyncRequest(oldLibrary, newLibrary, favoritesOnly)
        return Observable.create<Long> {
            service.startSyncTask(request).enqueue(object : Callback<StartSyncApiResponse> {
                override fun onResponse(call: Call<StartSyncApiResponse>?, response: Response<StartSyncApiResponse>?) {
                    val rawResponse = response?.body()
                    val responseSuccess = rawResponse?.success
                    val taskId = rawResponse?.task_id
                    if (responseSuccess == null || taskId == null || !responseSuccess) {
                        it.onError(RuntimeException("Sync failed!"))
                    } else {
                        it.onNext(taskId)
                        it.onCompleted()
                    }
                }

                override fun onFailure(call: Call<StartSyncApiResponse>?, t: Throwable?) {
                    it.onError(RuntimeException("Exception making HTTP request!", t))
                }
            })
        }
    }

    /**
     * Convert a task detail to a sync result (only call this when the sync is complete without errors)

     * @param taskDetails The task details
     * *
     * @return The sync result
     */
    private fun taskDetailsToSyncResult(taskDetails: JSONObject): SyncResult {
        try {
            val results = taskDetails.getJSONObject("result")
            val changesJSON = results.getJSONArray("changes")
            val conflictsJSON = results.getJSONArray("conflicts")
            val serializedLibrary = results.getString("library")
            val changes: MutableList<String> = ArrayList()
            val conflicts: MutableList<String> = ArrayList()
            for (i in 0..changesJSON.length() - 1) {
                changes.add(changesJSON.getString(i))
            }
            for (i in 0..conflictsJSON.length() - 1) {
                conflicts.add(conflictsJSON.getString(i))
            }
            return SyncResult.Success(changes = changes,
                    conflicts = conflicts,
                    serializedLibrary = serializedLibrary)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Get the details of a task from it's status

     * @param taskStatus The task status
     * *
     * @return The task details
     */
    private fun getTaskDetails(taskStatus: GetSyncStatusApiResponse): JSONObject {
        try {
            return JSONObject(taskStatus.details)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Check if the sync is complete from a task status

     * @param taskStatus The task status to check
     * *
     * @return Whether or not the sync is complete
     */
    private fun isSyncComplete(taskStatus: GetSyncStatusApiResponse, taskDetails: JSONObject): Boolean {
        try {
            if (taskStatus.complete!!) {
                return true
            }
            if (taskDetails.has("error")) {
                throw RuntimeException(
                        "Sync failed with error: '" + taskDetails.getString("error") + "'!")
            }
            return false
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }
}