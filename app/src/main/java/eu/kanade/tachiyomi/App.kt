package eu.kanade.tachiyomi

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.support.multidex.MultiDex
import com.evernote.android.job.JobManager
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.updater.UpdateCheckerJob
import eu.kanade.tachiyomi.util.LocaleHelper
import org.acra.ACRA
import org.acra.annotation.ReportsCrashes
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@ReportsCrashes(
        formUri = "http://tachiyomi.kanade.eu/crash_report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        buildConfigClass = BuildConfig::class,
        excludeMatchingSharedPreferencesKeys = arrayOf(".*username.*", ".*password.*", ".*token.*")
)
open class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.importModule(AppModule(this))

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        setupAcra()
        setupJobManager()

        LocaleHelper.updateCfg(this, baseContext.resources.configuration)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (BuildConfig.DEBUG) {
            MultiDex.install(this)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.updateCfg(this, newConfig)
    }

    protected open fun setupAcra() {
        ACRA.init(this)
    }

    protected open fun setupJobManager() {
        JobManager.create(this).addJobCreator { tag ->
            when (tag) {
                LibraryUpdateJob.TAG -> LibraryUpdateJob()
                UpdateCheckerJob.TAG -> UpdateCheckerJob()
                else -> null
            }
        }
    }

}
