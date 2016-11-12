package eu.kanade.tachiyomi.data.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo.State.CONNECTED
import android.net.NetworkInfo.State.DISCONNECTED
import android.os.IBinder
import android.os.PowerManager
import com.github.pwittchen.reactivenetwork.library.Connectivity
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.toast
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy

class DownloadService : Service() {

    companion object {

        val runningRelay: BehaviorRelay<Boolean> = BehaviorRelay.create()

        fun start(context: Context) {
            context.startService(Intent(context, DownloadService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DownloadService::class.java))
        }
    }

    val downloadManager: DownloadManager by injectLazy()
    val preferences: PreferencesHelper by injectLazy()

    private val wakeLock by lazy { (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DownloadService:WakeLock") }

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private var networkChangeSubscription: Subscription? = null
    private var queueRunningSubscription: Subscription? = null

    override fun onCreate() {
        super.onCreate()
        runningRelay.call(true)
        listenQueueRunningChanges()
        listenNetworkChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    override fun onDestroy() {
        runningRelay.call(false)
        queueRunningSubscription?.unsubscribe()
        networkChangeSubscription?.unsubscribe()
        downloadManager.stopDownloads()
        wakeLock.releaseIfNeeded()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun listenNetworkChanges() {
        networkChangeSubscription = ReactiveNetwork.observeNetworkConnectivity(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ state -> onNetworkStateChanged(state)
                }, { error ->
                    toast(R.string.download_queue_error)
                    stopSelf()
                })
    }

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

    private fun listenQueueRunningChanges() {
        queueRunningSubscription = downloadManager.runningSubject.subscribe { running ->
            if (running)
                wakeLock.acquireIfNeeded()
            else
                wakeLock.releaseIfNeeded()
        }
    }

    fun PowerManager.WakeLock.releaseIfNeeded() {
        if (isHeld) release()
    }

    fun PowerManager.WakeLock.acquireIfNeeded() {
        if (!isHeld) acquire()
    }

}
