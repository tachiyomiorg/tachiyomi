package eu.kanade.tachiyomi.ui.reader.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.FileProvider
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.notificationManager
import java.io.File

/**
 * The BroadcastReceiver of [ImageNotifier]
 * Intent calls should be made from this class.
 */
class ImageNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DELETE_IMAGE -> {
                deleteImage(intent.getStringExtra(EXTRA_FILE_LOCATION))
                context.notificationManager.cancel(intent.getIntExtra(NOTIFICATION_ID, 5))
            }
        }
    }

    /**
     * Called to delete image
     *
     * @param path path of file
     */
    private fun deleteImage(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }

    companion object {

        private const val ACTION_DELETE_IMAGE = "eu.kanade.DELETE_IMAGE"

        private const val EXTRA_FILE_LOCATION = "file_location"

        private const val NOTIFICATION_ID = "notification_id"

        /**
         * Called to start share intent to share image
         *
         * @param context context of application
         * @param path path of file
         * @param notificationId id of notification
         */
        internal fun shareImageIntent(context: Context, path: String, notificationId: Int): PendingIntent {
            val intent = Intent(Intent.ACTION_SEND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                putExtra(Intent.EXTRA_STREAM, Uri.parse(path))
                type = "image/*"
            }
            context.notificationManager.cancel(notificationId)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        /**
         * Called to show image in gallery application
         *
         * @param context context of application
         * @param path path of file
         */
        internal fun showImageIntent(context: Context, path: String): PendingIntent {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", File(path))
                setDataAndType(uri, "image/*")
            }
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal fun deleteImageIntent(context: Context, path: String, notificationId: Int): PendingIntent {
            val intent = Intent(context, ImageNotificationReceiver::class.java).apply {
                action = ACTION_DELETE_IMAGE
                putExtra(EXTRA_FILE_LOCATION, path)
                putExtra(NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
