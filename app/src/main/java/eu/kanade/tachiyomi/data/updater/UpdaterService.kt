package eu.kanade.tachiyomi.data.updater

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.util.getUriCompat
import eu.kanade.tachiyomi.util.saveTo
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File

class UpdaterService : IntentService(UpdaterService::class.java.name) {
    /**
     * Network helper
     */
    private val network: NetworkHelper by injectLazy()

    /**
     * Notifier for the updater state and progress.
     */
    private val notifier by lazy { UpdaterNotifier(this) }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val title = intent.getStringExtra(EXTRA_DOWNLOAD_TITLE) ?: getString(R.string.app_name)
        val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return
        downloadApk(title, url)
    }

    /**
     * Called to start downloading apk of new update
     *
     * @param url url location of file
     */
    private fun downloadApk(title: String, url: String) {
        // Show notification download starting.
        notifier.onDownloadStarted(title)

        val progressListener = object : ProgressListener {

            // Progress of the download
            var savedProgress = 0

            // Keep track of the last notification sent to avoid posting too many.
            var lastTick = 0L

            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                val progress = (100 * bytesRead / contentLength).toInt()
                val currentTime = System.currentTimeMillis()
                if (progress > savedProgress && currentTime - 200 > lastTick) {
                    savedProgress = progress
                    lastTick = currentTime
                    notifier.onProgressChange(progress)
                }
            }
        }

        try {
            // Download the new update.
            val response = network.client.newCallWithProgress(GET(url), progressListener).execute()

            // File where the apk will be saved.
            val apkFile = File(externalCacheDir, "update.apk")

            if (response.isSuccessful) {
                response.body()!!.source().saveTo(apkFile)
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }
            notifier.onDownloadFinished(apkFile.getUriCompat(this))
        } catch (error: Exception) {
            Timber.e(error)
            notifier.onDownloadError(url)
        }
    }

    companion object {
        /**
         * Download url.
         */
        internal const val EXTRA_DOWNLOAD_URL = "${BuildConfig.APPLICATION_ID}.UpdaterService.DOWNLOAD_URL"

        /**
         * Download title
         */
        internal const val EXTRA_DOWNLOAD_TITLE = "${BuildConfig.APPLICATION_ID}.UpdaterService.DOWNLOAD_TITLE"

        /**
         * Downloads a new update and let the user install the new version from a notification.
         * @param context the application context.
         * @param url the url to the new update.
         */
        fun downloadUpdate(context: Context, url: String, title: String = context.getString(R.string.app_name)) {
            val intent = Intent(context, UpdaterService::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_TITLE, title)
                putExtra(EXTRA_DOWNLOAD_URL, url)
            }
            context.startService(intent)
        }

        /**
         * Returns [PendingIntent] that starts a service which downloads the apk specified in url.
         *
         * @param url the url to the new update.
         * @return [PendingIntent]
         */
        internal fun downloadApkPendingService(context: Context, url: String): PendingIntent {
            val intent = Intent(context, UpdaterService::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, url)
            }
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}


