package xyz.nulldev.ts.sync

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import xyz.nulldev.ts.sync.model.GetSyncStatusApiResponse
import xyz.nulldev.ts.sync.model.StartSyncApiResponse
import xyz.nulldev.ts.sync.model.SyncRequest

/**
 * Retrofit sync service
 */
interface SyncService {
    @POST("/api/sync")
    fun startSyncTask(@Body syncRequest: SyncRequest): Call<StartSyncApiResponse>

    @GET("/api/task/{taskId}")
    fun getSyncStatus(@Path("taskId") taskId: Long): Call<GetSyncStatusApiResponse>
}