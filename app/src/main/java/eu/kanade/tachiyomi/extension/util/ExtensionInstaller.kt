package eu.kanade.tachiyomi.extension.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * The installer which installs, updates and uninstalls the extensions.
 *
 * @param context The application context.
 */
internal class ExtensionInstaller(private val context: Context) {

    /**
     * The system's download manager
     */
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /**
     * The broadcast receiver which listens to download completion events.
     */
    private val downloadReceiver = DownloadCompletionReceiver()

    /**
     * The currently requested downloads, with the package name (unique id) as key, and the id
     * returned by the download manager.
     */
    private val activeDownloads = hashMapOf<String, Long>()

    /**
     * Relay used to notify the installation step of every download.
     */
    private val downloadsRelay = PublishRelay.create<Pair<Long, InstallStep>>()

    /**
     * Adds the given extension to the downloads queue and returns an observable containing its
     * step in the installation process.
     *
     * @param url The url of the apk.
     * @param extension The extension to install.
     */
    fun downloadAndInstall(url: String, extension: Extension) = Observable.defer {
        val pkgName = extension.pkgName

        val oldDownload = activeDownloads[pkgName]
        if (oldDownload != null) {
            deleteDownload(pkgName)
        }

        // Register the receiver after removing (and unregistering) the previous download
        downloadReceiver.register()

        val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(extension.name)
                .setMimeType(APK_MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = downloadManager.enqueue(request)
        activeDownloads.put(pkgName, id)

        downloadsRelay.filter { it.first == id }
                .map { it.second }
                // Poll download status
                .mergeWith(pollStatus(id))
                // Force an error if the download takes more than 3 minutes
                .mergeWith(Observable.timer(3, TimeUnit.MINUTES).map { InstallStep.Error })
                // Stop when the application is installed or errors
                .takeUntil { it.isCompleted() }
                // Always notify on main thread
                .observeOn(AndroidSchedulers.mainThread())
                // Always remove the download when unsubscribed
                .doOnUnsubscribe { deleteDownload(pkgName) }
    }

    /**
     * Returns an observable that polls the given download id for its status every second, as the
     * manager doesn't have any notification system. It'll stop once the download finishes.
     *
     * @param id The id of the download to poll.
     */
    private fun pollStatus(id: Long): Observable<InstallStep> {
        val query = DownloadManager.Query().setFilterById(id)

        return Observable.interval(0, 1, TimeUnit.SECONDS)
                // Get the current download status
                .map {
                    downloadManager.query(query).use { cursor ->
                        cursor.moveToFirst()
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    }
                }
                // Ignore duplicate results
                .distinctUntilChanged()
                // Stop polling when the download fails or finishes
                .takeUntil { it == DownloadManager.STATUS_SUCCESSFUL || it == DownloadManager.STATUS_FAILED }
                // Map to our model
                .flatMap { status ->
                    when (status) {
                        DownloadManager.STATUS_PENDING -> Observable.just(InstallStep.Pending)
                        DownloadManager.STATUS_RUNNING -> Observable.just(InstallStep.Downloading)
                        else -> Observable.empty()
                    }
                }
    }

    /**
     * Starts an intent to install the extension at the given uri.
     *
     * @param uri The uri of the extension to install.
     */
    fun installApk(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(intent)
    }

    /**
     * Starts an intent to uninstall the extension by the given package name.
     *
     * @param pkgName The package name of the extension to uninstall
     */
    fun uninstallApk(pkgName: String) {
        val packageUri = Uri.parse("package:$pkgName")
        val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
        context.startActivity(uninstallIntent)
    }

    /**
     * Called when an extension is installed, allowing to update its installation step.
     *
     * @param pkgName The package name of the installed application.
     */
    fun onApkInstalled(pkgName: String) {
        val id = activeDownloads[pkgName] ?: return
        downloadsRelay.call(id to InstallStep.Installed)
    }

    /**
     * Deletes the download for the given package name.
     *
     * @param pkgName The package name of the download to delete.
     */
    fun deleteDownload(pkgName: String) {
        val downloadId = activeDownloads.remove(pkgName)
        if (downloadId != null) {
            downloadManager.remove(downloadId)
        }
        if (activeDownloads.isEmpty()) {
            downloadReceiver.unregister()
        }
    }

    /**
     * Receiver that listens to download status events.
     */
    private inner class DownloadCompletionReceiver : BroadcastReceiver() {

        /**
         * Whether this receiver is currently registered.
         */
        private var isRegistered = false

        /**
         * Registers this receiver if it's not already.
         */
        fun register() {
            if (isRegistered) return
            isRegistered = true

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            context.registerReceiver(this, filter)
        }

        /**
         * Unregisters this receiver if it's not already.
         */
        fun unregister() {
            if (!isRegistered) return
            isRegistered = false

            context.unregisterReceiver(this)
        }

        /**
         * Called when a download event is received. It looks for the download in the current active
         * downloads and notifies its installation step.
         */
        override fun onReceive(context: Context, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) ?: return

            // Avoid events for downloads we didn't request
            if (id !in activeDownloads.values) return

            val uri = downloadManager.getUriForDownloadedFile(id)
            if (uri != null) {
                downloadsRelay.call(id to InstallStep.Installing)
                installApk(uri)
            } else {
                Timber.e("Couldn't locate downloaded APK")
                downloadsRelay.call(id to InstallStep.Error)
            }
        }
    }

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
    }

}
