package eu.kanade.tachiyomi.data.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.github.pwittchen.reactivenetwork.library.ConnectivityStatus
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.toast
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy

class DownloadService : Service() {

    companion object {

        fun start(context: Context) {
            context.startService(Intent(context, DownloadService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DownloadService::class.java))
        }
    }

    val downloadManager: DownloadManager by injectLazy()
    val preferences: PreferencesHelper by injectLazy()

    private var wakeLock: PowerManager.WakeLock? = null
    private var networkChangeSubscription: Subscription? = null
    private var queueRunningSubscription: Subscription? = null
    private var isRunning: Boolean = false

    override fun onCreate() {
        super.onCreate()

        createWakeLock()

        listenQueueRunningChanges()
        listenNetworkChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    override fun onDestroy() {
        queueRunningSubscription?.unsubscribe()
        networkChangeSubscription?.unsubscribe()
        downloadManager.stopDownloads()
        destroyWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun listenNetworkChanges() {
        networkChangeSubscription = ReactiveNetwork().enableInternetCheck()
                .observeConnectivity(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ state ->
                    when (state) {
                        ConnectivityStatus.WIFI_CONNECTED_HAS_INTERNET -> {
                            // If there are no remaining downloads, destroy the service
                            if (!isRunning && !downloadManager.startDownloads()) {
                                stopSelf()
                            }
                        }
                        ConnectivityStatus.MOBILE_CONNECTED -> {
                            if (!preferences.downloadOnlyOverWifi()) {
                                if (!isRunning && !downloadManager.startDownloads()) {
                                    stopSelf()
                                }
                            } else if (isRunning) {
                                downloadManager.stopDownloads(getString(R.string.download_notifier_text_only_wifi))
                            }
                        }
                        else -> {
                            if (isRunning) {
                                downloadManager.stopDownloads(getString(R.string.download_notifier_text_only_wifi))
                            }
                        }
                    }
                }, { error ->
                    toast(R.string.download_queue_error)
                    stopSelf()
                })
    }

    private fun listenQueueRunningChanges() {
        queueRunningSubscription = downloadManager.runningSubject.subscribe { running ->
            isRunning = running
            if (running)
                acquireWakeLock()
            else
                releaseWakeLock()
        }
    }

    private fun createWakeLock() {
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "DownloadService:WakeLock")
    }

    private fun destroyWakeLock() {
        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock!!.release()
            wakeLock = null
        }
    }

    fun acquireWakeLock() {
        if (wakeLock != null && !wakeLock!!.isHeld) {
            wakeLock!!.acquire()
        }
    }

    fun releaseWakeLock() {
        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock!!.release()
        }
    }

}
