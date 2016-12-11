package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat
import eu.kanade.tachiyomi.Constants
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.util.chop
import eu.kanade.tachiyomi.util.notificationManager

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
        NotificationCompat.Builder(context)
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

    /**
     * Simultaneous download setting > 1.
     */
    var multipleDownloadThreads = false

    /**
     * Checks if error notification is visible
     */
    var errorThrown = false

    /**
     * Shows a notification from this builder.
     *
     * @param id the id of the notification.
     */
    private fun NotificationCompat.Builder.show(id: Int = Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ID) {
        context.notificationManager.notify(id, build())
    }

    /**
     * Dismiss the downloader's notification. Downloader error notifications use a different id, so
     * those can only be dismissed by the user.
     */
    fun dismiss() {
        context.notificationManager.cancel(Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ID)
    }

    /**
     * Called when download progress changes.
     * Note: Only accepted when multi download active.
     *
     * @param queue the queue containing downloads.
     */
    fun onProgressChange(queue: DownloadQueue) {
        if (multipleDownloadThreads) {
            doOnProgressChange(null, queue)
        }
    }

    /**
     * Called when download progress changes.
     * Note: Only accepted when single download active.
     *
     * @param download download object containing download information.
     * @param queue the queue containing downloads.
     */
    fun onProgressChange(download: Download, queue: DownloadQueue) {
        if (!multipleDownloadThreads) {
            doOnProgressChange(download, queue)
        }
    }

    /**
     * Show notification progress of chapter.
     *
     * @param download download object containing download information.
     * @param queue the queue containing downloads.
     */
    private fun doOnProgressChange(download: Download?, queue: DownloadQueue) {
        // Check if download is completed
        if (multipleDownloadThreads) {
            if (queue.isEmpty()) {
                onChapterCompleted(null)
                return
            }
        } else {
            if (download != null && download.pages!!.size == download.downloadedImages && queue.size - 1 == 0) {
                onChapterCompleted(download)
                return
            }
        }

        // Create notification
        with(notification) {
            // Check if icon needs refresh
            if (!isDownloading) {
                setSmallIcon(android.R.drawable.stat_sys_download)
                // Clear old actions if they exist
                if (!mActions.isEmpty())
                    mActions.clear()
                setContentIntent(DownloadNotificationReceiver.openDownloadManagerIntent(context))
                // Pause action
                addAction(R.drawable.ic_pause_grey_24dp_img,
                        context.getString(R.string.action_pause),
                        DownloadNotificationReceiver.pauseDownloadIntent(context))

                isDownloading = true
            }

            if (multipleDownloadThreads) {
                setContentTitle(context.getString(R.string.app_name))

                // Reset the queue size if the download progress is negative
                if ((initialQueueSize - queue.size) < 0)
                    initialQueueSize = queue.size

                setContentText(context.getString(R.string.chapter_downloading_progress)
                        .format(initialQueueSize - queue.size, initialQueueSize))
                setProgress(initialQueueSize, initialQueueSize - queue.size, false)
            } else {
                download?.let {
                    val title = it.manga.title
                    val chapter = it.chapter.name.replaceFirst(it.manga.title, "", true)
                    setContentTitle("$title - $chapter".chop(30))
                    setContentText(context.getString(R.string.chapter_downloading_progress)
                            .format(it.downloadedImages, it.pages!!.size))
                    setProgress(it.pages!!.size, it.downloadedImages, false)

                }
            }
        }
        // Displays the progress bar on notification
        notification.show()
    }

    /**
     * Show information when download is paused
     */
    fun onDownloadPaused() {
        // Create notification.
        with(notification) {
            setContentText(context.getString(R.string.update_check_notification_download_paused))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setProgress(0, 0, false)

            // Clear old actions if they exist
            if (!mActions.isEmpty())
                mActions.clear()
            setContentIntent(DownloadNotificationReceiver.openDownloadManagerIntent(context))
            // Resume action
            addAction(R.drawable.ic_play_arrow_grey_24dp_img,
                    context.getString(R.string.action_resume),
                    DownloadNotificationReceiver.resumeDownloadIntent(context))
            // Clear action
            addAction(R.drawable.ic_clear_grey_24dp_img,
                    context.getString(R.string.action_clear),
                    DownloadNotificationReceiver.clearDownloadIntent(context, Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ID))
        }
        // Show notification.
        notification.show()

        // Reset initial values
        isDownloading = false
    }

    /**
     * Called when chapter is downloaded.
     *
     * @param download download object containing download information.
     */
    private fun onChapterCompleted(download: Download?) {
        // Create notification.
        with(notification) {
            // Clear old actions if they exist
            if (!mActions.isEmpty())
                mActions.clear()

            if (download == null) {
                setContentTitle(context.getString(R.string.app_name))
                setContentIntent(DownloadNotificationReceiver.dismissNotification(context,
                        Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ID))
            } else {
                val title = download.manga.title
                val chapter = download.chapter.name.replaceFirst(download.manga.title, "", true)
                setContentTitle("$title - $chapter".chop(30))
                setContentIntent(DownloadNotificationReceiver.openManga(context, download.manga.id ?: 0,
                        download.chapter.id ?: 0, Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ID))
            }
            setContentText(context.getString(R.string.update_check_notification_download_complete))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
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
            setProgress(0, 0, false)
        }
        notification.show()
    }

    /**
     * Called when the downloader receives an error. It's shown as a separate notification to avoid
     * being overwritten.
     *
     * @param error string containing error information.
     * @param chapter string containing chapter title.
     */
    fun onError(error: String? = null, chapter: String? = null) {
        // Error is thrown
        errorThrown = true

        // Create notification
        with(notification) {
            setContentTitle(chapter ?: context.getString(R.string.download_notifier_downloader_title))
            setContentText(error ?: context.getString(R.string.download_notifier_unkown_error))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setProgress(0, 0, false)
        }
        notification.show(Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ERROR_ID)

        // Reset download information
        isDownloading = false
    }
}
