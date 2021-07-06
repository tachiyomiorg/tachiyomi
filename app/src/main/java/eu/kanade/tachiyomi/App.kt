package eu.kanade.tachiyomi

import android.app.ActivityManager
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.webkit.WebView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.multidex.MultiDex
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import eu.kanade.tachiyomi.data.coil.ByteBufferFetcher
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.notification
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.acra.config.httpSender
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import org.conscrypt.Conscrypt
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.security.Security

open class App : Application(), LifecycleObserver, ImageLoaderFactory {

    private val preferences: PreferencesHelper by injectLazy()

    private val disableIncognitoReceiver = DisableIncognitoReceiver()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        // TLS 1.3 support for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        // Avoid potential crashes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }

        Injekt.importModule(AppModule(this))

        setupAcra()
        setupNotificationChannels()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Show notification to disable Incognito Mode when it's enabled
        preferences.incognitoMode().asFlow()
            .onEach { enabled ->
                val notificationManager = NotificationManagerCompat.from(this)
                if (enabled) {
                    disableIncognitoReceiver.register()
                    val notification = notification(Notifications.CHANNEL_INCOGNITO_MODE) {
                        setContentTitle(getString(R.string.pref_incognito_mode))
                        setContentText(getString(R.string.notification_incognito_text))
                        setSmallIcon(R.drawable.ic_glasses_24dp)
                        setOngoing(true)

                        val pendingIntent = PendingIntent.getBroadcast(
                            this@App,
                            0,
                            Intent(ACTION_DISABLE_INCOGNITO_MODE),
                            PendingIntent.FLAG_ONE_SHOT
                        )
                        setContentIntent(pendingIntent)
                    }
                    notificationManager.notify(Notifications.ID_INCOGNITO_MODE, notification)
                } else {
                    disableIncognitoReceiver.unregister()
                    notificationManager.cancel(Notifications.ID_INCOGNITO_MODE)
                }
            }
            .launchIn(ProcessLifecycleOwner.get().lifecycleScope)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this).apply {
            componentRegistry {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder(this@App))
                } else {
                    add(GifDecoder())
                }
                add(ByteBufferFetcher())
                add(MangaCoverFetcher())
            }
            okHttpClient(Injekt.get<NetworkHelper>().coilClient)
            crossfade(300)
            allowRgb565(getSystemService<ActivityManager>()!!.isLowRamDevice)
        }.build()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @Suppress("unused")
    fun onAppBackgrounded() {
        if (preferences.lockAppAfter().get() >= 0) {
            SecureActivityDelegate.locked = true
        }
    }

    protected open fun setupAcra() {
        if (BuildConfig.FLAVOR != "dev") {
            initAcra {
                buildConfigClass = BuildConfig::class.java
                excludeMatchingSharedPreferencesKeys = arrayOf(".*username.*", ".*password.*", ".*token.*")

                httpSender {
                    uri = BuildConfig.ACRA_URI
                    httpMethod = HttpSender.Method.PUT
                }
            }
        }
    }

    protected open fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }

    private inner class DisableIncognitoReceiver : BroadcastReceiver() {
        private var registered = false

        override fun onReceive(context: Context, intent: Intent) {
            preferences.incognitoMode().set(false)
        }

        fun register() {
            if (!registered) {
                registerReceiver(this, IntentFilter(ACTION_DISABLE_INCOGNITO_MODE))
                registered = true
            }
        }

        fun unregister() {
            if (registered) {
                unregisterReceiver(this)
                registered = false
            }
        }
    }

    companion object {
        private const val ACTION_DISABLE_INCOGNITO_MODE = "tachi.action.DISABLE_INCOGNITO_MODE"
    }
}
