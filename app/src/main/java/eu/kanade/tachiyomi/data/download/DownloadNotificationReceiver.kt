package eu.kanade.tachiyomi.data.download

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.ui.download.DownloadActivity


/**
 * The BroadcastReceiver of [DownloadNotifier]
 * Intent calls should be made from this class.
 */
class DownloadNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_OPEN_DOWNLOAD_MANAGER -> {
                OpenDownloadManager(context)
            }
        }
    }

    private fun OpenDownloadManager(context: Context) {
        val intent = Intent(context, DownloadActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }


    companion object {
        private const val ACTION_OPEN_DOWNLOAD_MANAGER = "eu.kanade.ACTION_OPEN_DOWNLOAD_MANAGER"

        internal fun OpenDownloadManagerIntent(context: Context): PendingIntent {
            val intent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = ACTION_OPEN_DOWNLOAD_MANAGER
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
