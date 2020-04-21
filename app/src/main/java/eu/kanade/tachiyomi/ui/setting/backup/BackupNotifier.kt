package eu.kanade.tachiyomi.ui.setting.backup

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager

internal class BackupNotifier(private val context: Context) {

    private val notificationBuilder = context.notificationBuilder(Notifications.CHANNEL_BACKUP_RESTORE) {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
    }

    private fun NotificationCompat.Builder.show(id: Int = Notifications.ID_BACKUP) {
        context.notificationManager.notify(id, build())
    }

    fun dismiss() {
        context.notificationManager.cancel(Notifications.ID_BACKUP)
    }

    fun showBackupNotification() {
        with(notificationBuilder) {
            setSmallIcon(R.drawable.ic_tachi)
            setAutoCancel(false)

            setContentTitle(context.getString(R.string.backup))
            setContentText(context.getString(R.string.creating_backup))

            setProgress(0, 0, true)
        }

        notificationBuilder.show()
    }
}
