package eu.kanade.tachiyomi.util

import android.app.Application
import android.content.res.Configuration
import android.os.Build
import android.view.ContextThemeWrapper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy
import java.util.Locale


object LocaleHelper {

    private val preferences: PreferencesHelper by injectLazy()
    private var pLocale = Locale(LocaleHelper.intToLangCode(preferences.lang()))

    fun setLocale(locale: Locale) {
        pLocale = locale
        Locale.setDefault(pLocale)
    }

    fun updateCfg(wrapper: ContextThemeWrapper) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            val config = Configuration()
            config.setLocale(pLocale)
            wrapper.applyOverrideConfiguration(config)
        }
    }

    fun updateCfg(app: Application, config: Configuration){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.locale = pLocale
            app.baseContext.resources.updateConfiguration(config, app.baseContext.resources.displayMetrics)
        }
    }

    fun intToLangCode(i: Int): String {
        return when(i){
            1 -> "en"
            2 -> "es"
            3 -> "it"
            4 -> "pt"
            // System Language
            else -> ""
        }
    }

}
