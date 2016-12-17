package eu.kanade.tachiyomi.data.download

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.ui.download.DownloadActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.notificationManager
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ID as defaultNotificationID


/**
 * The BroadcastReceiver of [DownloadNotifier]
 * Intent calls should be made from this class.
 */
class DownloadNotificationReceiver : BroadcastReceiver() {
    /**
     * Download manager.
     */
    val downloadManager: DownloadManager by injectLazy()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PAUSE_DOWNLOAD -> {
                Timber.e("Pause action called")
                DownloadService.stop(context)
            }
            ACTION_RESUME_DOWNLOAD -> {
                Timber.e("Download action called")
                DownloadService.start(context)
            }
            ACTION_CLEAR_DOWNLOAD -> {
                DownloadService.stop(context)
                downloadManager.clearQueue()
                context.notificationManager.cancel(intent.getIntExtra(NOTIFICATION_ID, defaultNotificationID))
            }
            ACTION_DISMISS_NOTIFICATION -> context.notificationManager.cancel(intent.getIntExtra(NOTIFICATION_ID, defaultNotificationID))
        }
    }

    companion object {
        private const val ACTION_PAUSE_DOWNLOAD = "eu.kanade.ACTION_PAUSE_DOWNLOAD"
        private const val ACTION_RESUME_DOWNLOAD = "eu.kanade.ACTION_RESUME_DOWNLOAD"
        private const val ACTION_CLEAR_DOWNLOAD = "eu.kanade.ACTION_CLEAR_DOWNLOADS"
        private const val ACTION_DISMISS_NOTIFICATION = "eu.kanade.ACTION_DISMISS_NOTIFICATION"
        private const val NOTIFICATION_ID = "download_notification_id"

        internal fun openDownloadManagerIntent(context: Context): PendingIntent {
            val intent = Intent(context, DownloadActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal fun pauseDownloadIntent(context: Context): PendingIntent {
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_PAUSE_DOWNLOAD
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal fun resumeDownloadIntent(context: Context): PendingIntent {
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_RESUME_DOWNLOAD
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal fun clearDownloadIntent(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_CLEAR_DOWNLOAD
                putExtra(NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal fun openManga(context: Context, mangaId: Long, chapterId: Long, notificationId: Int): PendingIntent {
            val databaseHelper = DatabaseHelper(context)
            val manga = databaseHelper.getManga(mangaId).executeAsBlocking()
            val chapter = databaseHelper.getChapter(chapterId).executeAsBlocking()
            if (manga != null && chapter != null) {
                val intent = ReaderActivity.newIntent(context, manga, chapter)
                context.notificationManager.cancel(intent.getIntExtra(NOTIFICATION_ID, defaultNotificationID))
                return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            } else {
                return dismissNotification(context, notificationId)
            }
        }

        internal fun dismissNotification(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_DISMISS_NOTIFICATION
                putExtra(NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
