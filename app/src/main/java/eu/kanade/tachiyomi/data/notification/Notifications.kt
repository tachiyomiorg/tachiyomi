package eu.kanade.tachiyomi.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.notificationManager

/**
 * Class to manage the basic information of all the notifications used in the app.
 */
object Notifications {

    /**
     * Common notification channel and ids used anywhere.
     */
    const val CHANNEL_COMMON = "common_channel"
    const val ID_UPDATER = 1
    const val ID_DOWNLOAD_IMAGE = 2

    /**
     * Notification channel and ids used by the library updater.
     */
    const val CHANNEL_LIBRARY = "library_channel"
    const val ID_LIBRARY_PROGRESS = -101

    /**
     * Notification channel and ids used by the downloader.
     */
    const val CHANNEL_DOWNLOADER = "downloader_channel"
    const val ID_DOWNLOAD_CHAPTER = -201
    const val ID_DOWNLOAD_CHAPTER_ERROR = -202

    /**
     * Notification channel and ids used by the library updater.
     */
    const val CHANNEL_NEW_CHAPTERS = "new_chapters_channel"
    const val ID_NEW_CHAPTERS = -301
    const val GROUP_NEW_CHAPTERS = "eu.kanade.tachiyomi.NEW_CHAPTERS"

    /**
     * Notification channel and ids used by the library updater.
     */
    const val CHANNEL_UPDATES_TO_EXTS = "updates_ext_channel"
    const val ID_UPDATES_TO_EXTS = -401

    /**
     * Notification channel and ids used by the backup/restore system.
     */
    const val CHANNEL_BACKUP_RESTORE = "backup_restore_channel"
    const val ID_BACKUP = -501

    /**
     * Creates the notification channels introduced in Android Oreo.
     *
     * @param context The application context.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
                NotificationChannel(CHANNEL_COMMON, context.getString(R.string.channel_common),
                        NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_LIBRARY, context.getString(R.string.channel_library),
                        NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_DOWNLOADER, context.getString(R.string.channel_downloader),
                        NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_NEW_CHAPTERS, context.getString(R.string.channel_new_chapters),
                        NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_UPDATES_TO_EXTS, context.getString(R.string.channel_ext_updates),
                        NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_BACKUP_RESTORE, context.getString(R.string.channel_backup_restore),
                    NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                }
        )
        context.notificationManager.createNotificationChannels(channels)
    }
}
