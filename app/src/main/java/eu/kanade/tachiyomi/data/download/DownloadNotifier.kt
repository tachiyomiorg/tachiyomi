package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.chop
import eu.kanade.tachiyomi.util.notificationManager
import java.util.regex.Pattern

/**
 * DownloadNotifier is used to show notifications when downloading one or multiple chapters.
 *
 * @param context context of application
 */
internal class DownloadNotifier(private val context: Context) {
    /**
     * Notification builder.
     */
    private val notification by lazy {
        NotificationCompat.Builder(context, Notifications.CHANNEL_DOWNLOADER)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
    }

    /**
     * Status of download. Used for correct notification icon.
     */
    private var isDownloading = false

    /**
     * The size of queue on start download.
     */
    var initialQueueSize = 0
        set(value) {
            if (value != 0) {
                isSingleChapter = (value == 1)
            }
            field = value
        }

    /**
     * Updated when error is thrown
     */
    var errorThrown = false

    /**
     * Updated when only single page is downloaded
     */
    var isSingleChapter = false

    /**
     * Updated when paused
     */
    var paused = false

    /**
     * Shows a notification from this builder.
     *
     * @param id the id of the notification.
     */
    private fun NotificationCompat.Builder.show(id: Int = Notifications.ID_DOWNLOAD_CHAPTER) {
        context.notificationManager.notify(id, build())
    }

    /**
     * Clear old actions if they exist.
     */
    private fun clearActions() = with(notification) {
        if (!mActions.isEmpty())
            mActions.clear()
    }

    /**
     * Dismiss the downloader's notification. Downloader error notifications use a different id, so
     * those can only be dismissed by the user.
     */
    fun dismiss() {
        context.notificationManager.cancel(Notifications.ID_DOWNLOAD_CHAPTER)
    }

    /**
     * Called when download progress changes.
     *
     * @param download download object containing download information.
     */
    fun onProgressChange(download: Download) {
        // Create notification
        with(notification) {
            // Check if first call.
            if (!isDownloading) {
                setSmallIcon(android.R.drawable.stat_sys_download)
                setAutoCancel(false)
                clearActions()
                // Open download manager when clicked
                setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
                isDownloading = true
                // Pause action
                addAction(R.drawable.ic_av_pause_grey_24dp_img,
                        context.getString(R.string.action_pause),
                        NotificationReceiver.pauseDownloadsPendingBroadcast(context))
            }

            val title = download.manga.title.chop(15)
            val quotedTitle = Pattern.quote(title)
            val chapter = download.chapter.name.replaceFirst("$quotedTitle[\\s]*[-]*[\\s]*".toRegex(RegexOption.IGNORE_CASE), "")
            setContentTitle("$title - $chapter".chop(30))
            setContentText(context.getString(R.string.chapter_downloading_progress)
                    .format(download.downloadedImages, download.pages!!.size))
            setProgress(download.pages!!.size, download.downloadedImages, false)
        }
        // Displays the progress bar on notification
        notification.show()
    }

    /**
     * Show notification when download is paused.
     */
    fun onDownloadPaused() {
        with(notification) {
            setContentTitle(context.getString(R.string.chapter_paused))
            setContentText(context.getString(R.string.download_notifier_download_paused))
            setSmallIcon(R.drawable.ic_av_pause_grey_24dp_img)
            setAutoCancel(false)
            setProgress(0, 0, false)
            clearActions()
            // Open download manager when clicked
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
            // Resume action
            addAction(R.drawable.ic_av_play_arrow_grey_img,
                    context.getString(R.string.action_resume),
                    NotificationReceiver.resumeDownloadsPendingBroadcast(context))
            //Clear action
            addAction(R.drawable.ic_clear_grey_24dp_img,
                    context.getString(R.string.action_clear),
                    NotificationReceiver.clearDownloadsPendingBroadcast(context))
        }

        // Show notification.
        notification.show()

        // Reset initial values
        isDownloading = false
        initialQueueSize = 0
    }

    /**
     * Called when chapter is downloaded.
     *
     * @param download download object containing download information.
     */
    fun onDownloadCompleted(download: Download, queue: DownloadQueue) {
        // Check if last download
        if (!queue.isEmpty()) {
            return
        }
        // Create notification.
        with(notification) {
            val title = download.manga.title.chop(15)
            val quotedTitle = Pattern.quote(title)
            val chapter = download.chapter.name.replaceFirst("$quotedTitle[\\s]*[-]*[\\s]*".toRegex(RegexOption.IGNORE_CASE), "")
            setContentTitle("$title - $chapter".chop(30))
            setContentText(context.getString(R.string.update_check_notification_download_complete))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setAutoCancel(true)
            clearActions()
            setContentIntent(NotificationReceiver.openChapterPendingBroadcast(context, download.manga, download.chapter))
            setProgress(0, 0, false)
        }

        // Show notification.
        notification.show()

        // Reset initial values
        isDownloading = false
        initialQueueSize = 0
    }

    /**
     * Called when the downloader receives a warning.
     *
     * @param reason the text to show.
     */
    fun onWarning(reason: String) {
        with(notification) {
            setContentTitle(context.getString(R.string.download_notifier_downloader_title))
            setContentText(reason)
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setAutoCancel(true)
            clearActions()
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
            setProgress(0, 0, false)
        }
        notification.show()

        // Reset download information
        isDownloading = false
    }

    /**
     * Called when the downloader receives an error. It's shown as a separate notification to avoid
     * being overwritten.
     *
     * @param error string containing error information.
     * @param chapter string containing chapter title.
     */
    fun onError(error: String? = null, chapter: String? = null) {
        // Create notification
        with(notification) {
            setContentTitle(chapter ?: context.getString(R.string.download_notifier_downloader_title))
            setContentText(error ?: context.getString(R.string.download_notifier_unkown_error))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            clearActions()
            setAutoCancel(false)
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
            setProgress(0, 0, false)
        }
        notification.show(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)

        // Reset download information
        errorThrown = true
        isDownloading = false
    }
}
