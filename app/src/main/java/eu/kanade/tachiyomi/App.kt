package eu.kanade.tachiyomi

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
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
import eu.kanade.tachiyomi.util.system.LocaleHelper
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraHttpSender
import org.acra.sender.HttpSender
import org.conscrypt.Conscrypt
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.security.Security

@AcraCore(
    buildConfigClass = BuildConfig::class,
    excludeMatchingSharedPreferencesKeys = [".*username.*", ".*password.*", ".*token.*"]
)
@AcraHttpSender(
    uri = BuildConfig.ACRA_URI,
    httpMethod = HttpSender.Method.PUT
)
open class App : Application(), LifecycleObserver, ImageLoaderFactory {

    private val preferences: PreferencesHelper by injectLazy()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        // TLS 1.3 support for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        Injekt.importModule(AppModule(this))

        setupAcra()
        setupNotificationChannels()

        LocaleHelper.updateConfiguration(this, resources.configuration)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.updateConfiguration(this, newConfig, true)
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
            ACRA.init(this)
        }
    }

    protected open fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }
}
