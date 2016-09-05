package eu.kanade.tachiyomi.data.updater

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.Constants.NOTIFICATION_UPDATER_ID
import eu.kanade.tachiyomi.util.notificationManager
import java.io.File

class UpdateNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_INSTALL_APK -> {
                UpdateDownloaderService.installAPK(context,
                        File(intent.getStringExtra(EXTRA_FILE_LOCATION)))
                cancelNotification(context)
            }
            ACTION_DOWNLOAD_UPDATE -> UpdateDownloaderService.downloadUpdate(context,
                    intent.getStringExtra(UpdateDownloaderService.EXTRA_DOWNLOAD_URL))
            ACTION_CANCEL_NOTIFICATION -> cancelNotification(context)
        }
    }

    fun cancelNotification(context: Context) {
        context.notificationManager.cancel(NOTIFICATION_UPDATER_ID)
    }

    companion object {
        // Install apk action
        const val ACTION_INSTALL_APK = "eu.kanade.INSTALL_APK"

        // Download apk action
        const val ACTION_DOWNLOAD_UPDATE = "eu.kanade.RETRY_DOWNLOAD"

        // Cancel notification action
        const val ACTION_CANCEL_NOTIFICATION = "eu.kanade.CANCEL_NOTIFICATION"

        // Absolute path of apk file
        const val EXTRA_FILE_LOCATION = "file_location"

        fun cancelNotificationIntent(context: Context): PendingIntent {
            val intent = Intent(context, UpdateNotificationReceiver::class.java).apply {
                action = ACTION_CANCEL_NOTIFICATION
            }
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        fun installApkIntent(context: Context, path: String): PendingIntent {
            val intent = Intent(context, UpdateNotificationReceiver::class.java).apply {
                action = ACTION_INSTALL_APK
                putExtra(EXTRA_FILE_LOCATION, path)
            }
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        fun downloadApkIntent(context: Context, url: String): PendingIntent {
            val intent = Intent(context, UpdateNotificationReceiver::class.java).apply {
                action = ACTION_DOWNLOAD_UPDATE
                putExtra(UpdateDownloaderService.EXTRA_DOWNLOAD_URL, url)
            }
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }
    }

}