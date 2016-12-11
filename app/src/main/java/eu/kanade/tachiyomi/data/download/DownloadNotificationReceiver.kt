package eu.kanade.tachiyomi.data.download

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.Constants
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.download.DownloadActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.notificationManager
import uy.kohesive.injekt.injectLazy


/**
 * The BroadcastReceiver of [DownloadNotifier]
 * Intent calls should be made from this class.
 */
class DownloadNotificationReceiver : BroadcastReceiver() {
    /**
     * Download manager.
     */
    val downloadManager: DownloadManager by injectLazy()

    /**
     * Database Handler
     */
    val databaseHelper: DatabaseHelper by injectLazy()

    /**
     * The default id of the notification.
     */
    val defaultNotificationID = Constants.NOTIFICATION_DOWNLOAD_CHAPTER_ID

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_OPEN_DOWNLOAD_MANAGER -> openDownloadManager(context)
            ACTION_PAUSE_DOWNLOAD -> DownloadService.stop(context)
            ACTION_RESUME_DOWNLOAD -> DownloadService.start(context)
            ACTION_CLEAR_DOWNLOAD -> {
                DownloadService.stop(context)
                downloadManager.clearQueue()
                context.notificationManager.cancel(intent.getIntExtra(NOTIFICATION_ID, defaultNotificationID))
            }
            ACTION_OPEN_CHAPTER -> {
                val manga = databaseHelper.getManga(intent.getLongExtra(MANGA_ID, 0)).executeAsBlocking()
                val chapter = databaseHelper.getChapter(intent.getLongExtra(CHAPTER_ID, 0)).executeAsBlocking()
                openChapter(context, manga, chapter)
                context.notificationManager.cancel(intent.getIntExtra(NOTIFICATION_ID, defaultNotificationID))
            }
            ACTION_DISMISS_NOTIFICATION -> context.notificationManager.cancel(intent.getIntExtra(NOTIFICATION_ID, defaultNotificationID))
        }
    }

    private fun openDownloadManager(context: Context) {
        val intent = Intent(context, DownloadActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }


    private fun openChapter(context: Context, manga: Manga?, chapter: Chapter?, hasAnimation: Boolean = false) {
        if (manga != null && chapter != null) {
            val intent = ReaderActivity.newIntent(context, manga, chapter)
            if (hasAnimation) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
    }


    companion object {
        private const val ACTION_OPEN_DOWNLOAD_MANAGER = "eu.kanade.ACTION_OPEN_DOWNLOAD_MANAGER"
        private const val ACTION_PAUSE_DOWNLOAD = "eu.kanade.ACTION_PAUSE_DOWNLOAD"
        private const val ACTION_RESUME_DOWNLOAD = "eu.kanade.ACTION_RESUME_DOWNLOAD"
        private const val ACTION_CLEAR_DOWNLOAD = "eu.kanade.ACTION_CLEAR_DOWNLOADS"
        private const val ACTION_OPEN_CHAPTER = "eu.kanade.ACTION_OPEN_CHAPTER"
        private const val ACTION_DISMISS_NOTIFICATION = "eu.kanade.ACTION_DISMISS_NOTIFICATION"
        private const val NOTIFICATION_ID = "download_notification_id"
        private const val MANGA_ID = "mang_id"
        private const val CHAPTER_ID = "chapter_id"

        internal fun openDownloadManagerIntent(context: Context): PendingIntent {
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_OPEN_DOWNLOAD_MANAGER
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_OPEN_CHAPTER
                putExtra(MANGA_ID, mangaId)
                putExtra(CHAPTER_ID, chapterId)
                putExtra(NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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
