package eu.kanade.tachiyomi.data.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
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
    const val ID_LIBRARY_ERROR = -102

    /**
     * Notification channel and ids used by the downloader.
     */
    private const val GROUP_DOWNLOADER = "group_downloader"
    const val CHANNEL_DOWNLOADER_PROGRESS = "downloader_progress_channel"
    const val ID_DOWNLOAD_CHAPTER_PROGRESS = -201
    const val CHANNEL_DOWNLOADER_COMPLETE = "downloader_complete_channel"
    const val ID_DOWNLOAD_CHAPTER_COMPLETE = -203
    const val CHANNEL_DOWNLOADER_ERROR = "downloader_error_channel"
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
    private const val GROUP_BACKUP_RESTORE = "group_backup_restore"
    const val CHANNEL_BACKUP_RESTORE_PROGRESS = "backup_restore_progress_channel"
    const val ID_BACKUP_PROGRESS = -501
    const val ID_RESTORE_PROGRESS = -503
    const val CHANNEL_BACKUP_RESTORE_COMPLETE = "backup_restore_complete_channel_v2"
    const val ID_BACKUP_COMPLETE = -502
    const val ID_RESTORE_COMPLETE = -504

    /**
     * Notification channel used for crash log file sharing.
     */
    const val CHANNEL_CRASH_LOGS = "crash_logs_channel"
    const val ID_CRASH_LOGS = -601

    /**
     * Notification channel used for Incognito Mode
     */
    const val CHANNEL_INCOGNITO_MODE = "incognito_mode_channel"
    const val ID_INCOGNITO_MODE = -701

    private val deprecatedChannels = listOf(
        "downloader_channel",
        "backup_restore_complete_channel"
    )

    /**
     * Creates the notification channels introduced in Android Oreo.
     *
     * @param context The application context.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        listOf(
            NotificationChannelGroup(GROUP_BACKUP_RESTORE, context.getString(R.string.group_backup_restore)),
            NotificationChannelGroup(GROUP_DOWNLOADER, context.getString(R.string.group_downloader))
        ).forEach(context.notificationManager::createNotificationChannelGroup)

        listOf(
            NotificationChannel(
                CHANNEL_COMMON,
                context.getString(R.string.channel_common),
                NotificationManager.IMPORTANCE_LOW
            ),
            NotificationChannel(
                CHANNEL_LIBRARY,
                context.getString(R.string.channel_library),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_DOWNLOADER_PROGRESS,
                context.getString(R.string.channel_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                group = GROUP_DOWNLOADER
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_DOWNLOADER_COMPLETE,
                context.getString(R.string.channel_complete),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                group = GROUP_DOWNLOADER
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_DOWNLOADER_ERROR,
                context.getString(R.string.channel_errors),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                group = GROUP_DOWNLOADER
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_NEW_CHAPTERS,
                context.getString(R.string.channel_new_chapters),
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                CHANNEL_UPDATES_TO_EXTS,
                context.getString(R.string.channel_ext_updates),
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                CHANNEL_BACKUP_RESTORE_PROGRESS,
                context.getString(R.string.channel_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                group = GROUP_BACKUP_RESTORE
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_BACKUP_RESTORE_COMPLETE,
                context.getString(R.string.channel_complete),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                group = GROUP_BACKUP_RESTORE
                setShowBadge(false)
                setSound(null, null)
            },
            NotificationChannel(
                CHANNEL_CRASH_LOGS,
                context.getString(R.string.channel_crash_logs),
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                CHANNEL_INCOGNITO_MODE,
                context.getString(R.string.pref_incognito_mode),
                NotificationManager.IMPORTANCE_LOW
            )
        ).forEach(context.notificationManager::createNotificationChannel)

        // Delete old notification channels
        deprecatedChannels.forEach(context.notificationManager::deleteNotificationChannel)
    }
}
