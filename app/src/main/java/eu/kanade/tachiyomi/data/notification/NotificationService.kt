package eu.kanade.tachiyomi.data.notification

import android.app.IntentService
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.util.*
import java.io.File
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

/**
 * Class that manages [PendingIntent] and services of notifications.
 */
class NotificationService : IntentService(NotificationService::class.java.name) {

    companion object {
        // Name of Local BroadCastReceiver.
        private val INTENT_FILTER_NAME = NotificationService::class.java.name

        // Called to launch share intent.
        private val ACTION_SHARE_IMAGE = "$ID.$INTENT_FILTER_NAME.SHARE_IMAGE"

        // Called to delete image.
        private val ACTION_DELETE_IMAGE = "$ID.$INTENT_FILTER_NAME.DELETE_IMAGE"

        // Called to cancel library update.
        private val ACTION_CANCEL_LIBRARY_UPDATE = "$ID.$INTENT_FILTER_NAME.CANCEL_LIBRARY_UPDATE"

        // Value containing file location.
        private val EXTRA_FILE_LOCATION = "$ID.$INTENT_FILTER_NAME.FILE_LOCATION"

        // Value containing notification id.
        private val EXTRA_NOTIFICATION_ID = "$ID.$INTENT_FILTER_NAME.NOTIFICATION_ID"

        /**
         * Returns [PendingIntent] that starts a service which cancels the notification and starts a share activity
         *
         * @param context context of application
         * @param path location path of file
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun shareImagePendingService(context: Context, path: String, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationService::class.java).apply {
                action = ACTION_SHARE_IMAGE
                putExtra(EXTRA_FILE_LOCATION, path)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        /**
         * Returns [PendingIntent] that starts a service which removes an image from disk
         *
         * @param context context of application
         * @param path location path of file
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun deleteImagePendingService(context: Context, path: String, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationService::class.java).apply {
                action = ACTION_DELETE_IMAGE
                putExtra(EXTRA_FILE_LOCATION, path)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        /**
         * Returns [PendingIntent] that starts a service which stops the library update
         *
         * @param context context of application
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun cancelLibraryUpdatePendingService(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationService::class.java).apply {
                action = ACTION_CANCEL_LIBRARY_UPDATE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }
    }

    /**
     * Local [BroadcastReceiver] that runs on UI thread
     */
    private val notificationReceiver = NotificationServiceReceiver()

    override fun onCreate() {
        super.onCreate()
        // Register the local receiver
        registerLocalReceiver(notificationReceiver, IntentFilter(INTENT_FILTER_NAME))
    }

    override fun onDestroy() {
        // Unregister the local receiver
        unregisterLocalReceiver(notificationReceiver)
        super.onDestroy()
    }

    override fun onHandleIntent(intent: Intent) {
        when (intent.action) {
            // Launch share activity and dismiss notification
            ACTION_SHARE_IMAGE -> shareImage(this, intent.getStringExtra(EXTRA_FILE_LOCATION),
                    intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
            // Delete image from path and dismiss notification
            ACTION_DELETE_IMAGE -> deleteImage(this, intent.getStringExtra(EXTRA_FILE_LOCATION),
                    intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
            // Cancel library update and dismiss notification
            ACTION_CANCEL_LIBRARY_UPDATE -> cancelLibraryUpdate(this,
                    intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
        }
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
        dismissNotificationBroadcast(context, notificationId)
        // Launch share activity
        startActivity(intent)
    }

    /**
     * Called to delete image
     *
     * @param path path of file
     * @param notificationId id of notification
     */
    private fun deleteImage(context: Context, path: String, notificationId: Int) {
        // Delete file
        File(path).deleteIfExists()

        // Dismiss notification
        dismissNotificationBroadcast(context, notificationId)
    }

    /**
     * Method called when user wants to stop a library update
     *
     * @param context context of application
     * @param notificationId id of notification
     */
    private fun cancelLibraryUpdate(context: Context, notificationId: Int) {
        LibraryUpdateService.stop(context)
        dismissNotificationBroadcast(context, notificationId)
    }

    /**
     * Dismisses notification
     *
     * @param context context of application
     * @param notificationId id of notification
     */
    private fun dismissNotificationBroadcast(context: Context, notificationId: Int) {
        val intent = Intent(INTENT_FILTER_NAME).apply {
            putExtra(NotificationServiceReceiver.EXTRA_ACTION, NotificationServiceReceiver.ACTION_DISMISS_NOTIFICATION)
            putExtra(NotificationServiceReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        context.sendLocalBroadcast(intent)
    }
}

/**
 * Local [BroadcastReceiver] that runs on UI thread
 * Broadcasts of [NotificationService] should be made from here.
 */
class NotificationServiceReceiver : BroadcastReceiver() {

    companion object {
        private val NAME = NotificationReceiver::class.java.name

        // Called to dismiss notification.
        internal val ACTION_DISMISS_NOTIFICATION = "$ID.$NAME.DISMISS_NOTIFICATION"

        // Value containing notification id.
        internal val EXTRA_NOTIFICATION_ID = "$ID.$NAME.NOTIFICATION_ID"

        // Action Value
        internal val EXTRA_ACTION = "$ID.$NAME.ACTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra(EXTRA_ACTION)) {
            // Dismiss notification
            ACTION_DISMISS_NOTIFICATION -> context.notificationManager.cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
        }

    }
}

