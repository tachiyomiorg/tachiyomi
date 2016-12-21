package eu.kanade.tachiyomi.data.updater

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.Constants.NOTIFICATION_UPDATER_ID
import eu.kanade.tachiyomi.util.notificationManager
import java.io.File

class UpdateNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CANCEL_NOTIFICATION -> cancelNotification(context)
        }
    }

    companion object {
        // Cancel notification action
        const val ACTION_CANCEL_NOTIFICATION = "eu.kanade.CANCEL_NOTIFICATION"

        fun cancelNotificationIntent(context: Context): PendingIntent {
            val intent = Intent(context, UpdateNotificationReceiver::class.java).apply {
                action = ACTION_CANCEL_NOTIFICATION
            }
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        /**
         * Prompt user with apk install intent
         *
         * @param context context
         * @param file file of apk that is installed
         */
        fun installApkIntent(context: Context, file: File): PendingIntent {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
                else Uri.fromFile(file)
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            cancelNotification(context)
            return PendingIntent.getActivity(context, 0, intent, 0)
        }

        /**
         * Downloads a new update and let the user install the new version from a notification.
         *
         * @param context the application context.
         * @param url the url to the new update.
         */
        fun downloadApkIntent(context: Context, url: String): PendingIntent {
            val intent = Intent(context, UpdateDownloaderService::class.java).apply {
                putExtra(UpdateDownloaderService.EXTRA_DOWNLOAD_URL, url)
            }
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun cancelNotification(context: Context) {
            context.notificationManager.cancel(NOTIFICATION_UPDATER_ID)
        }
    }

}