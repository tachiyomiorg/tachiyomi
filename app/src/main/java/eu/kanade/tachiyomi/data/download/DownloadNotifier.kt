package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.support.v4.app.NotificationCompat
import eu.kanade.tachiyomi.Constants
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.util.notificationManager

/**
 * DownloadNotifier is used to show notifications when downloading one or multiple chapters.
 *
 * @param context context of application
 */
class DownloadNotifier(private val context: Context) {
    /**
     * Notification builder.
     */
    private val notificationBuilder = NotificationCompat.Builder(context)

    /**
     * Id of the notification.
     */
    private val notificationId: Int
        get() = Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ID

    /**
     * Status of download. Used for correct notification icon.
     */
    private var isDownloading = false

    /**
     * The size of queue on start download.
     */
    internal var initialQueueSize = 0

    /**
     * Simultaneous download setting > 1.
     */
    internal var multipleDownloadThreads = false

    /**
     * Called when download progress changes.
     * Note: Only accepted when multi download active.
     *
     * @param queue the queue containing downloads.
     */
    internal fun onProgressChange(queue: DownloadQueue) {
        if (multipleDownloadThreads) {
            doOnProgressChange(null, queue)
        }
    }

    /**
     * Called when download progress changes
     * Note: Only accepted when single download active
     *
     * @param download download object containing download information
     * @param queue the queue containing downloads
     */
    internal fun onProgressChange(download: Download, queue: DownloadQueue) {
        if (!multipleDownloadThreads) {
            doOnProgressChange(download, queue)
        }
    }

    /**
     * Show notification progress of chapter
     *
     * @param download download object containing download information
     * @param queue the queue containing downloads
     */
    private fun doOnProgressChange(download: Download?, queue: DownloadQueue) {
        // Check if download is completed
        if (multipleDownloadThreads) {
            if (queue.isEmpty()) {
                onComplete(null)
                return
            }
        } else {
            if (download != null && download.pages!!.size == download.downloadedImages) {
                onComplete(download)
                return
            }
        }

        // Create notification
        with (notificationBuilder) {
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
                    if (it.chapter.name.length >= 33)
                        setContentTitle(it.chapter.name.slice(IntRange(0, 30)).plus("..."))
                    else
                        setContentTitle(it.chapter.name)

                    setContentText(context.getString(R.string.chapter_downloading_progress)
                            .format(it.downloadedImages, it.pages!!.size))
                    setProgress(it.pages!!.size, it.downloadedImages, false)

                }
            }
        }
        // Displays the progress bar on notification
        context.notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Called when chapter is downloaded
     *
     * @param download download object containing download information
     */
    private fun onComplete(download: Download?) {
        // Create notification.
        with(notificationBuilder) {
            setContentTitle(download?.chapter?.name ?: context.getString(R.string.app_name))
            setContentText(context.getString(R.string.update_check_notification_download_complete))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setProgress(0, 0, false)
        }

        // Show notification.
        context.notificationManager.notify(notificationId, notificationBuilder.build())

        // Reset initial values
        isDownloading = false
        initialQueueSize = 0
    }

    /**
     * Clears the notification message
     */
    internal fun onClear() {
        context.notificationManager.cancel(notificationId)
    }

    /**
     * Called on error while downloading chapter
     *
     * @param error string containing error information
     * @param chapter string containing chapter title
     */
    internal fun onError(error: String? = null, chapter: String? = null) {
        // Create notification
        with(notificationBuilder) {
            setContentTitle(chapter ?: context.getString(R.string.download_notifier_title_error))
            setContentText(error ?: context.getString(R.string.download_notifier_unkown_error))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setProgress(0, 0, false)
        }
        context.notificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ERROR_ID, notificationBuilder.build())

        // Reset download information
        onClear()
        isDownloading = false
    }
}
