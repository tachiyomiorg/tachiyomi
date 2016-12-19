package eu.kanade.tachiyomi.data.download

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.download.DownloadActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import uy.kohesive.injekt.injectLazy

/**
 * The BroadcastReceiver of [DownloadNotifier]
 * Intent calls should be made from this class.
 */
class DownloadNotificationReceiver : BroadcastReceiver() {
    /**
     * Download manager.
     */
    private val downloadManager: DownloadManager by injectLazy()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PAUSE_DOWNLOADS -> downloadManager.pauseDownloads()
            ACTION_RESUME_DOWNLOADS -> DownloadService.start(context)
            ACTION_CLEAR_DOWNLOADS -> downloadManager.clearQueue(true)
        }
    }

    companion object {
        private const val ACTION_PAUSE_DOWNLOADS = "eu.kanade.ACTION_PAUSE_DOWNLOADS"

        private const val ACTION_RESUME_DOWNLOADS = "eu.kanade.ACTION_RESUME_DOWNLOADS"

        private const val ACTION_CLEAR_DOWNLOADS = "eu.kanade.ACTION_CLEAR_DOWNLOADS"

        /**
         * Returns [PendingIntent] that starts a broadcast to pause downloads.
         *
         * @param context context of application
         */
        internal fun pauseDownloadsBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_PAUSE_DOWNLOADS
            }
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        /**
         * Returns [PendingIntent] that starts a broadcast to resume downloads.
         *
         * @param context context of application
         */
        internal fun resumeDownloadsBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_RESUME_DOWNLOADS
            }
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        /**
         * Returns [PendingIntent] that starts a broadcast to clear downloads.
         *
         * @param context context of application
         */
        internal fun clearDownloadsBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_CLEAR_DOWNLOADS
            }
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        /**
         * Returns [PendingIntent] that start a reader activity containing chapter.
         *
         * @param context context of application
         * @param manga manga of chapter
         * @param chapter chapter that needs to be opened
         */
        internal fun openMangaIntent(context: Context, manga: Manga, chapter: Chapter): PendingIntent {
            val intent = ReaderActivity.newIntent(context, manga, chapter)
            return PendingIntent.getActivity(context, 0, intent, 0)
        }

        /**
         * Returns [PendingIntent] that starts a download activity.
         *
         * @param context context of application
         */
        internal fun openDownloadManagerIntent(context: Context): PendingIntent {
            val intent = Intent(context, DownloadActivity::class.java)
            return PendingIntent.getActivity(context, 0, intent, 0)
        }
    }

}