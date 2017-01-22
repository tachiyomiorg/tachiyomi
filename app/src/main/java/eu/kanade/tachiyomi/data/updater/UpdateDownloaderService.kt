package eu.kanade.tachiyomi.data.updater

import android.app.IntentService
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.util.registerLocalReceiver
import eu.kanade.tachiyomi.util.saveTo
import eu.kanade.tachiyomi.util.sendLocalBroadcastSync
import eu.kanade.tachiyomi.util.unregisterLocalReceiver
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File

class UpdateDownloaderService : IntentService(UpdateDownloaderService::class.java.name) {
    /**
     * Network helper
     */
    private val network: NetworkHelper by injectLazy()

    /**
     * Local [BroadcastReceiver] that runs on UI thread
     */
    private val updaterNotificationReceiver = UpdateDownloaderReceiver(this)


    override fun onCreate() {
        super.onCreate()
        // Register receiver
        registerLocalReceiver(updaterNotificationReceiver, IntentFilter(INTENT_FILTER_NAME))
    }

    override fun onDestroy() {
        // Unregister receiver
        unregisterLocalReceiver(updaterNotificationReceiver)
        super.onDestroy()
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return
        downloadApk(url)
    }

    /**
     * Called to start downloading apk of new update
     *
     * @param url url location of file
     */
    fun downloadApk(url: String) {
        // Show notification download starting.
        sendInitialBroadcast()
        // Progress of the download
        var savedProgress = 0

        val progressListener = object : ProgressListener {
            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                val progress = (100 * bytesRead / contentLength).toInt()
                if (progress > savedProgress) {
                    savedProgress = progress
                    sendProgressBroadcast(progress)
                }
            }
        }

        try {
            // Download the new update.
            val response = network.client.newCallWithProgress(GET(url), progressListener).execute()

            // File where the apk will be saved.
            val apkFile = File(externalCacheDir, "update.apk")

            if (response.isSuccessful) {
                response.body().source().saveTo(apkFile)
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }
            sendInstallBroadcast(apkFile.absolutePath)
        } catch (error: Exception) {
            Timber.e(error)
            sendErrorBroadcast(url)
        }
    }

    /**
     * Show notification download starting.
     */
    private fun sendInitialBroadcast() {
        val intent = Intent(INTENT_FILTER_NAME).apply {
            putExtra(UpdateDownloaderReceiver.EXTRA_ACTION, UpdateDownloaderReceiver.NOTIFICATION_UPDATER_INITIAL)
        }
        sendLocalBroadcastSync(intent)
    }

    /**
     * Show notification progress changed
     *
     * @param progress progress of download
     */
    private fun sendProgressBroadcast(progress: Int) {
        val intent = Intent(INTENT_FILTER_NAME).apply {
            putExtra(UpdateDownloaderReceiver.EXTRA_ACTION, UpdateDownloaderReceiver.NOTIFICATION_UPDATER_PROGRESS)
            putExtra(UpdateDownloaderReceiver.EXTRA_PROGRESS, progress)
        }
        // Prevents not showing of install notification TODO weird Android N bug. Find out what goes wrong
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || progress <= 95) {
            // Show download progress notification.
            sendLocalBroadcastSync(intent)
        }
    }

    /**
     * Show install notification.
     *
     * @param path location of file
     */
    private fun sendInstallBroadcast(path: String){
        val intent = Intent(INTENT_FILTER_NAME).apply {
            putExtra(UpdateDownloaderReceiver.EXTRA_ACTION, UpdateDownloaderReceiver.NOTIFICATION_UPDATER_INSTALL)
            putExtra(UpdateDownloaderReceiver.EXTRA_APK_PATH, path)
        }
        sendLocalBroadcastSync(intent)
    }

    /**
     * Show error notification.
     *
     * @param url url of file
     */
    private fun sendErrorBroadcast(url: String){
        val intent = Intent(INTENT_FILTER_NAME).apply {
            putExtra(UpdateDownloaderReceiver.EXTRA_ACTION, UpdateDownloaderReceiver.NOTIFICATION_UPDATER_ERROR)
            putExtra(UpdateDownloaderReceiver.EXTRA_APK_URL, url)
        }
        sendLocalBroadcastSync(intent)
    }

    companion object {
        /**
         * Name of Local BroadCastReceiver.
         */
        private val INTENT_FILTER_NAME = UpdateDownloaderService::class.java.name

        /**
         * Download url.
         */
        internal const val EXTRA_DOWNLOAD_URL = "${BuildConfig.APPLICATION_ID}.UpdateDownloaderService.DOWNLOAD_URL"

        /**
         * Downloads a new update and let the user install the new version from a notification.
         * @param context the application context.
         * @param url the url to the new update.
         */
        fun downloadUpdate(context: Context, url: String) {
            val intent = Intent(context, UpdateDownloaderService::class.java).apply {
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
            val intent = Intent(context, UpdateDownloaderService::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, url)
            }
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}


