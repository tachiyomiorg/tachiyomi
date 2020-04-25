package eu.kanade.tachiyomi.data.updater

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

class UpdaterJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        return runBlocking {
            try {
                val result = UpdateChecker.getUpdateChecker().checkForUpdate()

                if (result is UpdateResult.NewUpdate<*>) {
                    val url = result.release.downloadLink

                    val intent = Intent(context, UpdaterService::class.java).apply {
                        putExtra(UpdaterService.EXTRA_DOWNLOAD_URL, url)
                    }

                    NotificationCompat.Builder(context, Notifications.CHANNEL_COMMON).update {
                        setContentTitle(context.getString(R.string.app_name))
                        setContentText(context.getString(R.string.update_check_notification_update_available))
                        setSmallIcon(android.R.drawable.stat_sys_download_done)
                        // Download action
                        addAction(
                            android.R.drawable.stat_sys_download_done,
                            context.getString(R.string.action_download),
                            PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                        )
                    }
                }
                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }

    fun NotificationCompat.Builder.update(block: NotificationCompat.Builder.() -> Unit) {
        block()
        context.notificationManager.notify(Notifications.ID_UPDATER, build())
    }

    companion object {
        private const val TAG = "UpdateChecker"

        fun setupTask(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdaterJob>(
                3, TimeUnit.DAYS,
                3, TimeUnit.HOURS
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
