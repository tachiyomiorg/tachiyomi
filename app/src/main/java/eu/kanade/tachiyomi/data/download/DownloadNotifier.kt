package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import java.util.regex.Pattern
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * DownloadNotifier is used to show notifications when downloading one or multiple chapters.
 *
 * @param context context of application
 */
internal class DownloadNotifier(private val context: Context) {

    private val notificationBuilder = context.notificationBuilder(Notifications.CHANNEL_DOWNLOADER) {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
    }

    private val preferences by lazy { Injekt.get<PreferencesHelper>() }

    /**
     * Status of download. Used for correct notification icon.
     */
    @Volatile
    private var isDownloading = false

    /**
     * Updated when error is thrown
     */
    var errorThrown = false

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
    private fun clearActions() = with(notificationBuilder) {
        if (mActions.isNotEmpty()) {
            mActions.clear()
        }
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
        with(notificationBuilder) {
            // Check if first call.
            if (!isDownloading) {
                setSmallIcon(android.R.drawable.stat_sys_download)
                setAutoCancel(false)
                clearActions()
                // Open download manager when clicked
                setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
                isDownloading = true
                // Pause action
                addAction(
                    R.drawable.ic_pause_24dp,
                    context.getString(R.string.action_pause),
                    NotificationReceiver.pauseDownloadsPendingBroadcast(context)
                )
            }

            val downloadingProgressText = context.getString(
                R.string.chapter_downloading_progress, download.downloadedImages, download.pages!!.size
            )

            if (preferences.hideNotificationContent()) {
                setContentTitle(downloadingProgressText)
            } else {
                val title = download.manga.title.chop(15)
                val quotedTitle = Pattern.quote(title)
                val chapter = download.chapter.name.replaceFirst("$quotedTitle[\\s]*[-]*[\\s]*".toRegex(RegexOption.IGNORE_CASE), "")
                setContentTitle("$title - $chapter".chop(30))
                setContentText(downloadingProgressText)
            }

            setProgress(download.pages!!.size, download.downloadedImages, false)
        }

        // Displays the progress bar on notification
        notificationBuilder.show()
    }

    /**
     * Show notification when download is paused.
     */
    fun onDownloadPaused() {
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.chapter_paused))
            setContentText(context.getString(R.string.download_notifier_download_paused))
            setSmallIcon(R.drawable.ic_pause_24dp)
            setAutoCancel(false)
            setProgress(0, 0, false)
            clearActions()
            // Open download manager when clicked
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
            // Resume action
            addAction(
                R.drawable.ic_play_arrow_24dp,
                context.getString(R.string.action_resume),
                NotificationReceiver.resumeDownloadsPendingBroadcast(context)
            )
            // Clear action
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.action_cancel_all),
                NotificationReceiver.clearDownloadsPendingBroadcast(context)
            )
        }

        // Show notification.
        notificationBuilder.show()

        // Reset initial values
        isDownloading = false
    }

    /**
     * Called when the downloader receives a warning.
     *
     * @param reason the text to show.
     */
    fun onWarning(reason: String) {
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.download_notifier_downloader_title))
            setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setAutoCancel(true)
            clearActions()
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
            setProgress(0, 0, false)
        }
        notificationBuilder.show()

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
        with(notificationBuilder) {
            setContentTitle(
                chapter
                    ?: context.getString(R.string.download_notifier_downloader_title)
            )
            setContentText(error ?: context.getString(R.string.download_notifier_unknown_error))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            clearActions()
            setAutoCancel(false)
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
            setProgress(0, 0, false)
        }
        notificationBuilder.show(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)

        // Reset download information
        errorThrown = true
        isDownloading = false
    }
}
