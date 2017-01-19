package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.content.FileProvider
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.ui.download.DownloadActivity
import eu.kanade.tachiyomi.util.getUriCompat
import java.io.File

/**
 * Class that manages [PendingIntent] of activity's
 */
object NotificationHandler {
    /**
     * Returns [PendingIntent] that starts a download activity.
     *
     * @param context context of application
     */
    internal fun openDownloadManagerPendingActivity(context: Context): PendingIntent {
        val intent = Intent(context, DownloadActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        return PendingIntent.getActivity(context, 0, intent, 0)
    }

    /**
     * Returns [PendingIntent] that starts a gallery activity
     *
     * @param context context of application
     * @param file file containing image
     */
    internal fun openImagePendingActivity(context: Context, file: File): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
            setDataAndType(uri, "image/*")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Returns [PendingIntent] that prompts user with apk install intent
     *
     * @param context context
     * @param file file of apk that is installed
     */
    fun installApkPendingActivity(context: Context, file: File): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = file.getUriCompat(context)
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        return PendingIntent.getActivity(context, 0, intent, 0)
    }
}
