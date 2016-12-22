package eu.kanade.tachiyomi.data.updater

import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import eu.kanade.tachiyomi.Constants.NOTIFICATION_UPDATER_ID
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.notificationManager

class UpdateCheckerJob : Job() {

    override fun onRunJob(params: Params): Result {
        return GithubUpdateChecker()
                .checkForUpdate()
                .map { result ->
                    if (result is GithubUpdateResult.NewUpdate) {
                        val url = result.release.downloadLink

                        val intent = Intent(context, UpdateDownloaderService::class.java).apply {
                            putExtra(UpdateDownloaderService.EXTRA_DOWNLOAD_URL, url)
                        }

                        NotificationCompat.Builder(context).update {
                            setContentTitle(context.getString(R.string.app_name))
                            setContentText(context.getString(R.string.update_check_notification_update_available))
                            setSmallIcon(android.R.drawable.stat_sys_download_done)
                            // Download action
                            addAction(android.R.drawable.stat_sys_download_done,
                                    context.getString(R.string.action_download),
                                    PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                        }
                    }
                    Job.Result.SUCCESS
                }
                .onErrorReturn { Job.Result.FAILURE }
                // Sadly, the task needs to be synchronous.
                .toBlocking()
                .single()
    }

    fun NotificationCompat.Builder.update(block: NotificationCompat.Builder.() -> Unit) {
        block()
        context.notificationManager.notify(NOTIFICATION_UPDATER_ID, build())
    }

    companion object {
        const val TAG = "UpdateChecker"

        fun setupTask() {
            JobRequest.Builder(TAG)
                    .setPeriodic(24 * 60 * 60 * 1000, 60 * 60 * 1000)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .setPersisted(true)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()
        }

        fun cancelTask() {
            JobManager.instance().cancelAllForTag(TAG)
        }
    }

}