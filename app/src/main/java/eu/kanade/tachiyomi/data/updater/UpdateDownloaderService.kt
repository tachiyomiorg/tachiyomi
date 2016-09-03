package eu.kanade.tachiyomi.data.updater

import android.app.IntentService
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationCompat
import com.github.pwittchen.reactivenetwork.library.ConnectivityStatus
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork
import eu.kanade.tachiyomi.Constants.NOTIFICATION_UPDATER_ID
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.network.ProgressListener
import eu.kanade.tachiyomi.data.network.newCallWithProgress
import eu.kanade.tachiyomi.util.notificationManager
import eu.kanade.tachiyomi.util.saveTo
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File

class UpdateDownloaderService : IntentService(UpdateDownloaderService::class.java.name) {

    companion object {
        /**
         * Action to check for new updates.
         */
        const val ACTION_CHECK_VERSION = "eu.kanade.CHECK_UPDATE"

        /**
         * Action to download a new update.
         */
        const val ACTION_DOWNLOAD_UPDATE = "eu.kanade.DOWNLOAD_UPDATE"

        /**
         * Look for updates and notify the user if there's a new version with a notification.
         * @param context the application context.
         * @param onlyWifi whether to look for updates only with wifi.
         */
        fun checkVersion(context: Context, onlyWifi: Boolean = false) {
            if (onlyWifi) {
                val connection = ReactiveNetwork().getConnectivityStatus(context, true)
                if (connection != ConnectivityStatus.WIFI_CONNECTED_HAS_INTERNET)
                    return
            }
            val intent = Intent(context, UpdateDownloaderService::class.java).apply {
                action = ACTION_CHECK_VERSION
            }
            context.startService(intent)
        }

        /**
         * Downloads a new update and let the user install the new version from a notification.
         * @param context the application context.
         * @param url the url to the new update.
         */
        fun downloadUpdate(context: Context, url: String) {
            val intent = Intent(context, UpdateDownloaderService::class.java).apply {
                action = ACTION_DOWNLOAD_UPDATE
                putExtra(Intent.EXTRA_TEXT, url)
            }
            context.startService(intent)
        }

        /**
         * Prompt user with apk install intent
         * @param context context
         * @param file file of apk that is installed
         */
        fun installAPK(context: Context, file: File) {
            // Prompt install interface
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                // Without this flag android returned a intent error!
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Network helper
     */
    private val network: NetworkHelper by injectLazy()

    init {
        setIntentRedelivery(true)
    }


    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            ACTION_DOWNLOAD_UPDATE -> {
                val url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                downloadApk(url)
            }
            ACTION_CHECK_VERSION -> {
                checkVersion()
            }
        }
    }

    fun downloadApk(url: String) {
        val progressNotification = NotificationCompat.Builder(this)

        progressNotification.update {
            setOngoing(true)
            setContentTitle(getString(R.string.update_check_notification_file_download))
            setContentText(getString(R.string.update_check_notification_download_in_progress))
            setSmallIcon(android.R.drawable.stat_sys_download)
        }

        // Progress of the download
        var savedProgress = 0

        val progressListener = object : ProgressListener {
            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                val progress = (100 * bytesRead / contentLength).toInt()
                if (progress > savedProgress) {
                    savedProgress = progress

                    progressNotification.update { setProgress(100, progress, false) }
                }
            }
        }

        try {
            // Download the new update.
            val response = network.client.newCallWithProgress(GET(url), progressListener).execute()

            // File where the apk will be saved
            val apkFile = File(externalCacheDir, "update.apk")

            if (response.isSuccessful) {
                response.body().source().saveTo(apkFile)
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }

            // Prompt the user to install the new update.
            NotificationCompat.Builder(this).update {
                setContentTitle(getString(R.string.app_name))
                setContentText(getString(R.string.update_check_notification_download_complete))
                setSmallIcon(android.R.drawable.stat_sys_download_done)
                // Install action
                addAction(R.drawable.ic_system_update_grey_24dp_img,
                        getString(R.string.action_install),
                        getInstallApkIntent(apkFile.absolutePath))
                // Cancel action
                addAction(R.drawable.ic_clear_grey_24dp_img,
                        getString(R.string.action_cancel),
                        getCancelNotificationIntent())
            }

        } catch (e: Exception) {
            Timber.e(e, e.message)

            // Prompt the user to retry the download.
            NotificationCompat.Builder(this).update {
                setContentText(getString(R.string.update_check_notification_download_error))
                setSmallIcon(android.R.drawable.stat_sys_download_done)
                // Retry action
                addAction(R.drawable.ic_refresh_grey_24dp_img,
                        getString(R.string.action_retry),
                        getDownloadUpdateIntent(url))
                // Cancel action
                addAction(R.drawable.ic_clear_grey_24dp_img,
                        getString(R.string.action_cancel),
                        getCancelNotificationIntent())
            }
        }
    }

    fun checkVersion() {
        GithubUpdateChecker().checkForUpdate()
                .subscribe({ result ->
                    if (result is GithubUpdateResult.NewUpdate) {
                        val url = result.release.downloadLink

                        NotificationCompat.Builder(this).update {
                            setContentTitle(getString(R.string.update_check_notification_update_available))
                            setSmallIcon(android.R.drawable.stat_sys_download_done)
                            // Download action
                            addAction(android.R.drawable.stat_sys_download_done,
                                    getString(R.string.action_download),
                                    getDownloadUpdateIntent(url))
                        }
                    }
                }, { error ->
                    Timber.e(error, error.message)
                })
    }

    fun NotificationCompat.Builder.update(block: NotificationCompat.Builder.() -> Unit) {
        block()
        notificationManager.notify(NOTIFICATION_UPDATER_ID, build())
    }

    private fun getDownloadUpdateIntent(url: String): PendingIntent {
        val intent = Intent(this, UpdaterReceiver::class.java).apply {
            action = UpdaterReceiver.ACTION_DOWNLOAD_UPDATE
            putExtra(UpdaterReceiver.FILE_LOCATION, url)
        }
        return PendingIntent.getBroadcast(this, 0, intent, 0)
    }

    private fun getCancelNotificationIntent(): PendingIntent {
        val intent = Intent(this, UpdaterReceiver::class.java).apply {
            action = UpdaterReceiver.ACTION_CANCEL_NOTIFICATION
        }
        return PendingIntent.getBroadcast(this, 0, intent, 0)
    }

    private fun getInstallApkIntent(path: String): PendingIntent {
        val intent = Intent(this, UpdaterReceiver::class.java).apply {
            action = UpdaterReceiver.ACTION_INSTALL_APK
            putExtra(UpdaterReceiver.FILE_LOCATION, path)
        }
        return PendingIntent.getBroadcast(this, 0, intent, 0)
    }

    /**
     * BroadcastReceiver used to handle updater events.
     */
    class UpdaterReceiver : BroadcastReceiver() {
        companion object {
            // Install apk action
            const val ACTION_INSTALL_APK = "eu.kanade.INSTALL_APK"

            // Retry download action
            const val ACTION_DOWNLOAD_UPDATE = "eu.kanade.DOWNLOAD_UPDATE"

            // Retry download action
            const val ACTION_CANCEL_NOTIFICATION = "eu.kanade.CANCEL_NOTIFICATION"

            // Absolute path of file || URL of file
            const val FILE_LOCATION = "file_location"
        }

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_INSTALL_APK -> {
                    installAPK(context, File(intent.getStringExtra(FILE_LOCATION)))
                    cancelNotification(context)
                }
                ACTION_DOWNLOAD_UPDATE -> downloadUpdate(context, intent.getStringExtra(FILE_LOCATION))
                ACTION_CANCEL_NOTIFICATION -> cancelNotification(context)
            }
        }

        fun cancelNotification(context: Context) {
            context.notificationManager.cancel(NOTIFICATION_UPDATER_ID)
        }

    }

}