package eu.kanade.tachiyomi.ui.setting.backup

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager

internal class BackupNotifier(private val context: Context) {

    private val notificationBuilder = context.notificationBuilder(Notifications.CHANNEL_BACKUP_RESTORE) {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
    }

    private fun NotificationCompat.Builder.show(id: Int) {
        context.notificationManager.notify(id, build())
    }

    fun showBackupProgress() {
        with(notificationBuilder) {
            setSmallIcon(R.drawable.ic_tachi)
            setAutoCancel(false)

            setContentTitle(context.getString(R.string.backup))
            setContentText(context.getString(R.string.creating_backup))

            setProgress(0, 0, true)
        }

        notificationBuilder.show(Notifications.ID_BACKUP)
    }

    fun showBackupError(error: String) {
        with(notificationBuilder) {
            setSmallIcon(R.drawable.ic_tachi)
            setAutoCancel(false)

            setContentTitle(context.getString(R.string.creating_backup_error))
            setContentText(error)

            // Remove progress bar
            setProgress(0, 0, false)
        }

        notificationBuilder.show(Notifications.ID_BACKUP)
    }

    fun showBackupComplete(unifile: UniFile) {
        with(notificationBuilder) {
            setSmallIcon(R.drawable.ic_tachi)
            setAutoCancel(false)

            setContentTitle(context.getString(R.string.backup_created))

            if (unifile.filePath != null) {
                setContentText(context.getString(R.string.file_saved, unifile.filePath))
            }

            // Remove progress bar
            setProgress(0, 0, false)

            // Clear old actions if they exist
            if (mActions.isNotEmpty()) {
                mActions.clear()
            }

            addAction(R.drawable.ic_share_24dp,
                context.getString(R.string.action_share),
                NotificationReceiver.shareBackup(context, unifile.uri, Notifications.ID_BACKUP))
        }

        notificationBuilder.show(Notifications.ID_BACKUP)
    }
}
