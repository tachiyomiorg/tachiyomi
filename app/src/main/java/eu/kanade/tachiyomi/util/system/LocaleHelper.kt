package eu.kanade.tachiyomi.util.system

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.ContextThemeWrapper
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.browse.source.SourcePresenter
import uy.kohesive.injekt.injectLazy
import java.util.Locale

/**
 * Utility class to change the application's language in runtime.
 */
@Suppress("DEPRECATION")
object LocaleHelper {

    private val preferences: PreferencesHelper by injectLazy()

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
    fun getLocaleFromString(pref: String?): Locale? {
        if (pref.isNullOrEmpty()) {
            return null
        }
        return getLocale(pref)
    }

    /**
     * Returns Display name of a string language code
     */
    fun getSourceDisplayName(lang: String?, context: Context): String {
        return when (lang) {
            "" -> context.getString(R.string.other_source)
            SourcePresenter.LAST_USED_KEY -> context.getString(R.string.last_used_source)
            SourcePresenter.PINNED_KEY -> context.getString(R.string.pinned_sources)
            "all" -> context.getString(R.string.all_lang)
            else -> getDisplayName(lang)
        }
    }

    /**
     * Returns Display name of a string language code
     */
    fun getDisplayName(lang: String?): String {
        return when (lang) {
            null -> ""
            "" -> {
                systemLocale!!.getDisplayName(systemLocale).capitalize()
            }
            else -> {
                val locale = getLocale(lang)
                locale.getDisplayName(locale).capitalize()
            }
        }
    }

    /**
     * Return Locale from string language code
     */
    private fun getLocale(lang: String): Locale {
        val sp = lang.split("_", "-")
        return when (sp.size) {
            2 -> Locale(sp[0], sp[1])
            3 -> Locale(sp[0], sp[1], sp[2])
            else -> Locale(lang)
        }
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
        if (appLocale != null) {
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

        Locale.setDefault(currentLocale)
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
            newConfig.setLocale(locale)
        }
        return newConfig
    }
}
