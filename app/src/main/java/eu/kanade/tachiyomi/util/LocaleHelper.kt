package eu.kanade.tachiyomi.util

import android.app.Application
import android.content.res.Configuration
import android.os.Build
import android.view.ContextThemeWrapper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy
import java.util.*

/**
 * Utility class to change the application's language in runtime.
 */
@Suppress("DEPRECATION")
object LocaleHelper {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * In API 16 and below the application's configuration has to be changed, so we need a copy of
     * the initial locale. The only problem is that if the system locale changes while the app is
     * running, it won't change until an application restart.
     */
    private var v16SystemLocale = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
        preferences.context.resources.configuration.locale else null

    /**
     * The application's locale. When it's null, the system locale is used.
     */
    private var appLocale = getLocaleFromCode(preferences.lang())

    /**
     * Returns the locale for the value stored in preferences, or null if system language or unknown
     * value is selected.
     *
     * @param pref the int value stored in preferences.
     */
    private fun getLocaleFromCode(pref: Int): Locale? {
        val code = when(pref) {
            1 -> "en"
            2 -> "es"
            3 -> "it"
            4 -> "pt"
            else -> return null
        }

        return Locale(code)
    }

    /**
     * Changes the application's locale with a new preference.
     *
     * @param pref the new value stored in preferences.
     */
    fun changeLocale(pref: Int) {
        appLocale = getLocaleFromCode(pref)
    }

    /**
     * Updates the app's language from API 17.
     */
    fun updateCfg(wrapper: ContextThemeWrapper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && appLocale != null) {
            val config = Configuration(preferences.context.resources.configuration)
            config.setLocale(appLocale)
            wrapper.applyOverrideConfiguration(config)
        }
    }

    /**
     * Updates the app's language for API 16 and lower.
     */
    fun updateCfg(app: Application, config: Configuration) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val configCopy = Configuration(config)
            val displayMetrics = app.baseContext.resources.displayMetrics
            configCopy.locale = appLocale ?: v16SystemLocale
            app.baseContext.resources.updateConfiguration(configCopy, displayMetrics)
        }
    }


}
