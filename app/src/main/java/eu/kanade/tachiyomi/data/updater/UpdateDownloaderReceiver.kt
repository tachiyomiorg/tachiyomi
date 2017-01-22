package eu.kanade.tachiyomi.data.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import eu.kanade.tachiyomi.Constants
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.util.notificationManager
import java.io.File
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

/**
 * Local [BroadcastReceiver] that runs on UI thread
 * Notification calls from [UpdateDownloaderService] should be made from here.
 */
internal class UpdateDownloaderReceiver(val context: Context) : BroadcastReceiver() {

    companion object {
        private const val NAME = "UpdateDownloaderReceiver"

        // Called to show initial notification.
        internal const val NOTIFICATION_UPDATER_INITIAL = "$ID.$NAME.UPDATER_INITIAL"

        // Called to show progress notification.
        internal const val NOTIFICATION_UPDATER_PROGRESS = "$ID.$NAME.UPDATER_PROGRESS"

        // Called to show install notification.
        internal const val NOTIFICATION_UPDATER_INSTALL = "$ID.$NAME.UPDATER_INSTALL"

        // Called to show error notification
        internal const val NOTIFICATION_UPDATER_ERROR = "$ID.$NAME.UPDATER_ERROR"

        // Value containing action of BroadcastReceiver
        internal const val EXTRA_ACTION = "$ID.$NAME.ACTION"

        // Value containing progress
        internal const val EXTRA_PROGRESS = "$ID.$NAME.PROGRESS"

        // Value containing apk path
        internal const val EXTRA_APK_PATH = "$ID.$NAME.APK_PATH"

        // Value containing apk url
        internal const val EXTRA_APK_URL = "$ID.$NAME.APK_URL"
    }

    /**
     * Notification shown to user
     */
    private val notification = NotificationCompat.Builder(context)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra(EXTRA_ACTION)) {
            NOTIFICATION_UPDATER_INITIAL -> basicNotification()
            NOTIFICATION_UPDATER_PROGRESS -> updateProgress(intent.getIntExtra(EXTRA_PROGRESS, 0))
            NOTIFICATION_UPDATER_INSTALL -> installNotification(intent.getStringExtra(EXTRA_APK_PATH))
            NOTIFICATION_UPDATER_ERROR -> errorNotification(intent.getStringExtra(EXTRA_APK_URL))
        }
    }

    /**
     * Called to show basic notification
     */
    private fun basicNotification() {
        // Create notification
        with(notification) {
            setContentTitle(context.getString(R.string.app_name))
            setContentText(context.getString(R.string.update_check_notification_download_in_progress))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)
        }
        notification.show()
    }

    /**
     * Called to show progress notification
     *
     * @param progress progress of download
     */
    private fun updateProgress(progress: Int) {
        with(notification) {
            setProgress(100, progress, false)
        }
        notification.show()
    }

    /**
     * Called to show install notification
     *
     * @param path path of file
     */
    private fun installNotification(path: String) {
        // Prompt the user to install the new update.
        with(notification) {
            setContentText(context.getString(R.string.update_check_notification_download_complete))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setProgress(0, 0, false)
            // Install action
            setContentIntent(NotificationHandler.installApkPendingActivity(context, File(path)))
            addAction(R.drawable.ic_system_update_grey_24dp_img,
                    context.getString(R.string.action_install),
                    NotificationHandler.installApkPendingActivity(context, File(path)))
            // Cancel action
            addAction(R.drawable.ic_clear_grey_24dp_img,
                    context.getString(R.string.action_cancel),
                    NotificationReceiver.dismissNotificationPendingBroadcast(context, Constants.NOTIFICATION_UPDATER_ID))
        }
        notification.show()
    }

    /**
     * Called to show error notification
     *
     * @param url url of apk
     */
    private fun errorNotification(url: String) {
        // Prompt the user to retry the download.
        with(notification) {
            setContentText(context.getString(R.string.update_check_notification_download_error))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setProgress(0, 0, false)
            // Retry action
            addAction(R.drawable.ic_refresh_grey_24dp_img,
                    context.getString(R.string.action_retry),
                    UpdateDownloaderService.downloadApkPendingService(context, url))
            // Cancel action
            addAction(R.drawable.ic_clear_grey_24dp_img,
                    context.getString(R.string.action_cancel),
                    NotificationReceiver.dismissNotificationPendingBroadcast(context, Constants.NOTIFICATION_UPDATER_ID))
        }
        notification.show()
    }

    /**
     * Shows a notification from this builder.
     *
     * @param id the id of the notification.
     */
    private fun NotificationCompat.Builder.show(id: Int = Constants.NOTIFICATION_UPDATER_ID) {
        context.notificationManager.notify(id, build())
    }
}
