package eu.kanade.tachiyomi.data.updater

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import eu.kanade.tachiyomi.Constants.NOTIFICATION_UPDATER_ID
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.network.ProgressListener
import eu.kanade.tachiyomi.data.network.newCallWithProgress
import eu.kanade.tachiyomi.util.notificationManager
import eu.kanade.tachiyomi.util.saveTo
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File

class UpdateDownloaderService : IntentService(UpdateDownloaderService::class.java.name) {

    companion object {
        /**
         * Download url.
         */
        const val EXTRA_DOWNLOAD_URL = "eu.kanade.APP_DOWNLOAD_URL"

        /**
         * Downloads a new update and let the user install the new version from a notification.
         * @param context the application context.
         * @param url the url to the new update.
         */
        fun downloadUpdate(context: Context, url: String) {
            val intent = Intent(context, UpdateDownloaderService::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, url)
            }
            context.startService(intent)
        }
    }

    /**
     * Network helper
     */
    private val network: NetworkHelper by injectLazy()

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return
        downloadApk(url)
    }

    fun downloadApk(url: String) {
        val progressNotification = NotificationCompat.Builder(this)

        progressNotification.update {
            setContentTitle(getString(R.string.app_name))
            setContentText(getString(R.string.update_check_notification_download_in_progress))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)
        }

        // Progress of the download
        var savedProgress = 0

        val progressListener = object : ProgressListener {
            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                val progress = (100 * bytesRead / contentLength).toInt()
                if (progress > savedProgress) {
                    savedProgress = progress

                    progressNotification.update { setProgress(100, progress, false) }
                }
            }
        }

        // Reference the context for later usage inside apply blocks.
        val ctx = this

        try {
            // Download the new update.
            val response = network.client.newCallWithProgress(GET(url), progressListener).execute()

            // File where the apk will be saved
            val apkFile = File(externalCacheDir, "update.apk")

            if (response.isSuccessful) {
                response.body().source().saveTo(apkFile)
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }

            val installIntent = UpdateNotificationReceiver.installApkIntent(ctx, apkFile)

            // Prompt the user to install the new update.
            NotificationCompat.Builder(this).update {
                setContentTitle(getString(R.string.app_name))
                setContentText(getString(R.string.update_check_notification_download_complete))
                setSmallIcon(android.R.drawable.stat_sys_download_done)
                // Install action
                setContentIntent(installIntent)
                addAction(R.drawable.ic_system_update_grey_24dp_img,
                        getString(R.string.action_install),
                        installIntent)
                // Cancel action
                addAction(R.drawable.ic_clear_grey_24dp_img,
                        getString(R.string.action_cancel),
                        UpdateNotificationReceiver.cancelNotificationIntent(ctx))
            }

        } catch (error: Exception) {
            Timber.e(error)

            // Prompt the user to retry the download.
            NotificationCompat.Builder(this).update {
                setContentTitle(getString(R.string.app_name))
                setContentText(getString(R.string.update_check_notification_download_error))
                setSmallIcon(android.R.drawable.stat_sys_download_done)
                // Retry action
                addAction(R.drawable.ic_refresh_grey_24dp_img,
                        getString(R.string.action_retry),
                        UpdateNotificationReceiver.downloadApkIntent(ctx, url))
                // Cancel action
                addAction(R.drawable.ic_clear_grey_24dp_img,
                        getString(R.string.action_cancel),
                        UpdateNotificationReceiver.cancelNotificationIntent(ctx))
            }
        }
    }

    fun NotificationCompat.Builder.update(block: NotificationCompat.Builder.() -> Unit) {
        block()
        notificationManager.notify(NOTIFICATION_UPDATER_ID, build())
    }

}