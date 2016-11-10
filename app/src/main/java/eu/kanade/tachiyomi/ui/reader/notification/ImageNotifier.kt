package eu.kanade.tachiyomi.ui.reader.notification

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import eu.kanade.tachiyomi.Constants
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.notificationManager
import java.io.File

/**
 * Class used to show BigPictureStyle notifications
 */
class ImageNotifier(private val context: Context) {
    /**
     * Notification builder.
     */
    private val notificationBuilder = NotificationCompat.Builder(context)

    /**
     * Id of the notification.
     */
    private val notificationId: Int
        get() = Constants.NOTIFICATION_DOWNLOAD_IMAGE_ID

    /**
     * Called when image download/copy is complete
     * @param file image file containing downloaded page image
     */
    fun onComplete(file: File) {

        Glide.with(context).load(file).asBitmap().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(object : SimpleTarget<Bitmap>(720, 1280) {
            /**
             * The method that will be called when the resource load has finished.
             * @param resource the loaded resource.
             */
            override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
                if (resource!= null){
                    showCompleteNotification(file, resource)
                }else{
                    onError(null)
                }
            }
        })
    }

    private fun showCompleteNotification(file: File, image: Bitmap) {
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.picture_saved))
            setSmallIcon(R.drawable.ic_insert_photo_white_24dp)
            setStyle(NotificationCompat.BigPictureStyle().bigPicture(image))
            setLargeIcon(image)
            setAutoCancel(true)
            // Clear old actions if they exist
            if (!mActions.isEmpty())
                mActions.clear()

            setContentIntent(ImageNotificationReceiver.showImageIntent(context, file.absolutePath))
            // Share action
            addAction(R.drawable.ic_share_grey_24dp,
                    context.getString(R.string.action_share),
                    ImageNotificationReceiver.shareImageIntent(context, file.absolutePath, notificationId))
            // Delete action
            addAction(R.drawable.ic_delete_grey_24dp,
                    context.getString(R.string.action_delete),
                    ImageNotificationReceiver.deleteImageIntent(context, file.absolutePath, notificationId))
            updateNotification()

        }
    }

    /**
     * Clears the notification message
     */
    fun onClear() {
        context.notificationManager.cancel(notificationId)
    }

    private fun updateNotification() {
        // Displays the progress bar on notification
        context.notificationManager.notify(notificationId, notificationBuilder.build())
    }


    /**
     * Called on error while downloading image
     * @param error string containing error information
     */
    fun onError(error: String?) {
        // Create notification
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.download_notifier_title_error))
            setContentText(error ?: context.getString(R.string.unknown_error))
            setSmallIcon(android.R.drawable.ic_menu_report_image)
        }
        updateNotification()
    }

}
