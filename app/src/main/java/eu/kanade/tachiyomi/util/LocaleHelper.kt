package eu.kanade.tachiyomi.util

import android.app.Application
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
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
     * The system's locale.
     */
    private var systemLocale: Locale? = null

    /**
     * The application's locale. When it's null, the system locale is used.
     */
    private var appLocale = getLocaleFromString(preferences.lang())

    /**
     * The currently applied locale. Used to avoid losing the selected language after a non locale
     * configuration change to the application.
     */
    private var currentLocale: Locale? = null

    /**
     * Returns the locale for the value stored in preferences, or null if it's system language.
     *
     * @param pref the string value stored in preferences.
     */
    fun getLocaleFromString(pref: String): Locale? {
        if (pref.isNullOrEmpty()) {
            return null
        }
        val parts = pref.split("_", "-")
        val lang = parts[0]
        val country = parts.getOrNull(1) ?: ""
        return Locale(lang, country)
    }

    /**
     * Changes the application's locale with a new preference.
     *
     * @param pref the new value stored in preferences.
     */
    fun changeLocale(pref: String) {
        appLocale = getLocaleFromString(pref)
    }

    /**
     * Updates the app's language to an activity.
     */
    fun updateConfiguration(wrapper: ContextThemeWrapper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && appLocale != null) {
            val config = Configuration(preferences.context.resources.configuration)
            config.setLocale(appLocale)
            wrapper.applyOverrideConfiguration(config)
        }
    }

    /**
     * Updates the app's language to the application.
     */
    fun updateConfiguration(app: Application, config: Configuration, configChange: Boolean = false) {
        if (systemLocale == null) {
            systemLocale = getConfigLocale(config)
        }
        if (configChange) {
            val configLocale = getConfigLocale(config)
            if (currentLocale == configLocale) {
                return
            }
            systemLocale = configLocale
        }
        currentLocale = appLocale ?: systemLocale ?: Locale.getDefault()
        val newConfig = updateConfigLocale(config, currentLocale!!)
        val resources = app.resources
        resources.updateConfiguration(newConfig, resources.displayMetrics)
    }

    /**
     * Returns the locale applied in the given configuration.
     */
    private fun getConfigLocale(config: Configuration): Locale {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            config.locale
        } else {
            config.locales[0]
        }
    }

    /**
     * Returns a new configuration with the given locale applied.
     */
    private fun updateConfigLocale(config: Configuration, locale: Locale): Configuration {
        val newConfig = Configuration(config)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            newConfig.locale = locale
        } else {
            newConfig.locales = LocaleList(locale)
        }
        return newConfig
    }

}
