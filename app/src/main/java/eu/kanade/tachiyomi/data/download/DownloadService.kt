package eu.kanade.tachiyomi.data.download

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo.State.CONNECTED
import android.net.NetworkInfo.State.DISCONNECTED
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.app.NotificationCompat
import com.github.pwittchen.reactivenetwork.library.Connectivity
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.connectivityManager
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.util.powerManager
import eu.kanade.tachiyomi.util.toast
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.injectLazy

/**
 * This service is used to manage the downloader. The system can decide to stop the service, in
 * which case the downloader is also stopped. It's also stopped while there's no network available.
 * While the downloader is running, a wake lock will be held.
 */
class DownloadService : Service() {

    companion object {

        /**
         * Relay used to know when the service is running.
         */
        val runningRelay: BehaviorRelay<Boolean> = BehaviorRelay.create(false)

        /**
         * Starts this service.
         *
         * @param context the application context.
         */
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.startService(intent)
            } else {
                context.startForegroundService(intent)
            }
        }

        /**
         * Stops this service.
         *
         * @param context the application context.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, DownloadService::class.java))
        }
    }

    /**
     * Download manager.
     */
    private val downloadManager: DownloadManager by injectLazy()

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Wake lock to prevent the device to enter sleep mode.
     */
    private val wakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DownloadService:WakeLock")
    }

    /**
     * Subscriptions to store while the service is running.
     */
    private lateinit var subscriptions: CompositeSubscription

    /**
     * Called when the service is created.
     */
    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.ID_DOWNLOAD_CHAPTER, getPlaceholderNotification())
        runningRelay.call(true)
        subscriptions = CompositeSubscription()
        listenDownloaderState()
        listenNetworkChanges()
    }

    /**
     * Called when the service is destroyed.
     */
    override fun onDestroy() {
        runningRelay.call(false)
        subscriptions.unsubscribe()
        downloadManager.stopDownloads()
        wakeLock.releaseIfNeeded()
        super.onDestroy()
    }

    /**
     * Not used.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return Service.START_NOT_STICKY
    }

    /**
     * Not used.
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Listens to network changes.
     *
     * @see onNetworkStateChanged
     */
    private fun listenNetworkChanges() {
        subscriptions += ReactiveNetwork.observeNetworkConnectivity(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ state -> onNetworkStateChanged(state)
                }, { _ ->
                    toast(R.string.download_queue_error)
                    stopSelf()
                })
    }

    /**
     * Called when the network state changes.
     *
     * @param connectivity the new network state.
     */
    private fun onNetworkStateChanged(connectivity: Connectivity) {
        when (connectivity.state) {
            CONNECTED -> {
                if (preferences.downloadOnlyOverWifi() && connectivityManager.isActiveNetworkMetered) {
                    downloadManager.stopDownloads(getString(R.string.download_notifier_text_only_wifi))
                } else {
                    val started = downloadManager.startDownloads()
                    if (!started) stopSelf()
                }
            }
            DISCONNECTED -> {
                downloadManager.stopDownloads(getString(R.string.download_notifier_no_network))
            }
            else -> { /* Do nothing */ }
        }
    }

    /**
     * Listens to downloader status. Enables or disables the wake lock depending on the status.
     */
    private fun listenDownloaderState() {
        subscriptions += downloadManager.runningRelay.subscribe { running ->
            if (running)
                wakeLock.acquireIfNeeded()
            else
                wakeLock.releaseIfNeeded()
        }
    }

    /**
     * Releases the wake lock if it's held.
     */
    fun PowerManager.WakeLock.releaseIfNeeded() {
        if (isHeld) release()
    }

    /**
     * Acquires the wake lock if it's not held.
     */
    fun PowerManager.WakeLock.acquireIfNeeded() {
        if (!isHeld) acquire()
    }

    private fun getPlaceholderNotification(): Notification {
        return NotificationCompat.Builder(this, Notifications.CHANNEL_DOWNLOADER)
            .setContentTitle(getString(R.string.download_notifier_downloader_title))
            .build()
    }

}
