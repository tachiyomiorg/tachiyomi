package eu.kanade.tachiyomi.data.updater

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class UpdaterJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork() = runBlocking {
        try {
            val result = GithubUpdateChecker().checkForUpdate()

            if (result is GithubUpdateResult.NewUpdate) {
                UpdaterNotifier(context).promptUpdate(result.release.getDownloadLink())
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "UpdateChecker"

        fun setupTask(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdaterJob>(
                3,
                TimeUnit.DAYS,
                3,
                TimeUnit.HOURS
            )
                .addTag(TAG)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request)
        }

        fun cancelTask(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        }
    }
}
