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
            if (download != null && download.pages!!.size == download.downloadedImages) {
                onChapterCompleted(download)
                return
            }
        }

        // Create notification
        with(notification) {
            // Check if icon needs refresh
            if (!isDownloading) {
                setSmallIcon(android.R.drawable.stat_sys_download)
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
                    setContentTitle(it.chapter.name.chop(30))
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
     * Called when chapter is downloaded.
     *
     * @param download download object containing download information.
     */
    private fun onChapterCompleted(download: Download?) {
        // Create notification.
        with(notification) {
            setContentTitle(download?.chapter?.name ?: context.getString(R.string.app_name))
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
