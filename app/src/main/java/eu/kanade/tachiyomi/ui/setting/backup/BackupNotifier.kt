package eu.kanade.tachiyomi.ui.setting.backup

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import java.io.File
import java.util.concurrent.TimeUnit

internal class BackupNotifier(private val context: Context) {

    private val notificationBuilder = context.notificationBuilder(Notifications.CHANNEL_BACKUP_RESTORE) {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        setSmallIcon(R.drawable.ic_tachi)
        setAutoCancel(false)
    }

    private fun NotificationCompat.Builder.show(id: Int) {
        context.notificationManager.notify(id, build())
    }

    fun showBackupProgress() {
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.backup))
            setContentText(context.getString(R.string.creating_backup))

            setProgress(0, 0, true)
            setOngoing(true)
        }

        notificationBuilder.show(Notifications.ID_BACKUP)
    }

    fun showBackupError(error: String?) {
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.creating_backup_error))
            setContentText(error)

            // Remove progress bar
            setProgress(0, 0, false)
            setOngoing(false)
        }

        notificationBuilder.show(Notifications.ID_BACKUP)
    }

    fun showBackupComplete(unifile: UniFile) {
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.backup_created))

            if (unifile.filePath != null) {
                setContentText(context.getString(R.string.file_saved, unifile.filePath))
            }

            // Remove progress bar
            setProgress(0, 0, false)
            setOngoing(false)

            // Clear old actions if they exist
            if (mActions.isNotEmpty()) {
                mActions.clear()
            }

            addAction(
                R.drawable.ic_share_24dp,
                context.getString(R.string.action_share),
                NotificationReceiver.shareBackupPendingBroadcast(context, unifile.uri, Notifications.ID_BACKUP)
            )
        }

        notificationBuilder.show(Notifications.ID_BACKUP)
    }

    fun showRestoreProgress(content: String = "", progress: Int = 0, maxAmount: Int = 100): NotificationCompat.Builder {
        val builder = with(notificationBuilder) {
            setContentTitle(context.getString(R.string.restoring_backup))
            setContentText(content)

            setProgress(maxAmount, progress, false)
            setOngoing(true)

            // Clear old actions if they exist
            if (mActions.isNotEmpty()) {
                mActions.clear()
            }

            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.action_stop),
                NotificationReceiver.cancelRestorePendingBroadcast(context, Notifications.ID_RESTORE)
            )
        }

        builder.show(Notifications.ID_RESTORE)

        return builder
    }

    fun showRestoreError(error: String?) {
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.restoring_backup_error))
            setContentText(error)

            // Remove progress bar
            setProgress(0, 0, false)
            setOngoing(false)
        }

        notificationBuilder.show(Notifications.ID_RESTORE)
    }

    fun showRestoreComplete(time: Long, errorCount: Int, path: String?, file: String?) {
        val timeString = context.getString(
            R.string.restore_duration,
            TimeUnit.MILLISECONDS.toMinutes(time),
            TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(time)
            )
        )

        with(notificationBuilder) {
            setSmallIcon(R.drawable.ic_tachi)
            setAutoCancel(false)

            setContentTitle(context.getString(R.string.restore_completed))
            setContentText(context.getString(R.string.restore_completed_content, timeString, errorCount))

            // Remove progress bar
            setProgress(0, 0, false)
            setOngoing(false)

            // Clear old actions if they exist
            if (mActions.isNotEmpty()) {
                mActions.clear()
            }

            if (errorCount > 0 && !path.isNullOrEmpty() && !file.isNullOrEmpty()) {
                val destFile = File(path, file)
                val uri = destFile.getUriCompat(context)

                addAction(
                    R.drawable.nnf_ic_file_folder,
                    context.getString(R.string.action_open_log),
                    NotificationReceiver.openErrorLogPendingActivity(context, uri)
                )
            }
        }

        notificationBuilder.show(Notifications.ID_RESTORE)
    }
}
