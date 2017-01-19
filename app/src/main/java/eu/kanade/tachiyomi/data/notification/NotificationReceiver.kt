package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.util.deleteIfExists
import eu.kanade.tachiyomi.util.getUriCompat
import eu.kanade.tachiyomi.util.notificationManager
import uy.kohesive.injekt.injectLazy
import java.io.File
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

/**
 * Global [BroadcastReceiver] that runs on UI thread
 * Pending Broadcasts should be made from here.
 * NOTE: Use local broadcasts if possible.
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private val NAME = NotificationReceiver::class.java.name

        // Called to launch share intent.
        private val ACTION_SHARE_IMAGE = "$ID.$NAME.SHARE_IMAGE"

        // Called to delete image.
        private val ACTION_DELETE_IMAGE = "$ID.$NAME.DELETE_IMAGE"

        // Called to cancel library update.
        private val ACTION_CANCEL_LIBRARY_UPDATE = "$ID.$NAME.CANCEL_LIBRARY_UPDATE"

        // Value containing file location.
        private val EXTRA_FILE_LOCATION = "$ID.$NAME.FILE_LOCATION"

        // Called to resume downloads.
        private val ACTION_RESUME_DOWNLOADS = "$ID.$NAME.ACTION_RESUME_DOWNLOADS"

        // Called to clear downloads.
        private val ACTION_CLEAR_DOWNLOADS = "$ID.$NAME.ACTION_CLEAR_DOWNLOADS"

        // Called to dismiss notification.
        private val ACTION_DISMISS_NOTIFICATION = "$ID.$NAME.ACTION_DISMISS_NOTIFICATION"

        // Value containing notification id.
        private val EXTRA_NOTIFICATION_ID = "$ID.$NAME.NOTIFICATION_ID"

        /**
         * Returns a [PendingIntent] that resumes the download of a chapter
         *
         * @param context context of application
         * @return [PendingIntent]
         */
        internal fun resumeDownloadsPendingBroadcast(context: Context): PendingIntent? {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_RESUME_DOWNLOADS
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        /**
         * Returns a [PendingIntent] that clears the download queue
         *
         * @param context context of application
         * @return [PendingIntent]
         */
        internal fun clearDownloadsPendingBroadcast(context: Context): PendingIntent? {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_CLEAR_DOWNLOADS
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        /**
         * Returns [PendingIntent] that starts a service which dismissed the notification
         *
         * @param context context of application
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun dismissNotificationPendingBroadcast(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_DISMISS_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        /**
         * Returns [PendingIntent] that starts a service which cancels the notification and starts a share activity
         *
         * @param context context of application
         * @param path location path of file
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun shareImagePendingBroadcast(context: Context, path: String, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_SHARE_IMAGE
                putExtra(EXTRA_FILE_LOCATION, path)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        /**
         * Returns [PendingIntent] that starts a service which removes an image from disk
         *
         * @param context context of application
         * @param path location path of file
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun deleteImagePendingBroadcast(context: Context, path: String, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_DELETE_IMAGE
                putExtra(EXTRA_FILE_LOCATION, path)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        /**
         * Returns [PendingIntent] that starts a service which stops the library update
         *
         * @param context context of application
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun cancelLibraryUpdatePendingBroadcast(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_CANCEL_LIBRARY_UPDATE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }
    }
    /**
     * Download manager.
     */
    private val downloadManager: DownloadManager by injectLazy()

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action){
            // Dismiss notification
            ACTION_DISMISS_NOTIFICATION -> dismissNotification(context,intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
            // Resume the download service
            ACTION_RESUME_DOWNLOADS -> DownloadService.start(context)
            // Clear the download queue
            ACTION_CLEAR_DOWNLOADS -> downloadManager.clearQueue(true)
            // Launch share activity and dismiss notification
            ACTION_SHARE_IMAGE -> shareImage(context, intent.getStringExtra(EXTRA_FILE_LOCATION),
                    intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
            // Delete image from path and dismiss notification
            ACTION_DELETE_IMAGE -> deleteImage(context, intent.getStringExtra(EXTRA_FILE_LOCATION),
                    intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
            // Cancel library update and dismiss notification
            ACTION_CANCEL_LIBRARY_UPDATE -> cancelLibraryUpdate(context,
                    intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
        }
    }

    /**
     * Dismiss the notification
     *
     * @param notificationId the id of the notification
     */
    private fun dismissNotification(context: Context, notificationId: Int){
        context.notificationManager.cancel(notificationId)
    }

    /**
     * Called to start share intent to share image
     *
     * @param context context of application
     * @param path path of file
     * @param notificationId id of notification
     */
    private fun shareImage(context: Context, path: String, notificationId: Int) {
        // Create intent
        val intent = Intent(Intent.ACTION_SEND).apply {
            val uri = File(path).getUriCompat(context)
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "image/*"
        }
        // Dismiss notification
        dismissNotification(context, notificationId)
        // Launch share activity
        context.startActivity(intent)
    }

    /**
     * Called to delete image
     *
     * @param path path of file
     * @param notificationId id of notification
     */
    private fun deleteImage(context: Context, path: String, notificationId: Int) {
        // Dismiss notification
        dismissNotification(context, notificationId)

        // Delete file
        File(path).deleteIfExists()
    }

    /**
     * Method called when user wants to stop a library update
     *
     * @param context context of application
     * @param notificationId id of notification
     */
    private fun cancelLibraryUpdate(context: Context, notificationId: Int) {
        LibraryUpdateService.stop(context)
        dismissNotification(context, notificationId)
    }
}