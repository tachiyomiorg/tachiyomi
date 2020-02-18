package eu.kanade.tachiyomi.ui.base.activity

import android.app.UiModeManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.LocaleHelper
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values

abstract class BaseActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(when (preferences.themeMode()) {
            Values.THEME_MODE_LIGHT -> R.style.Theme_Tachiyomi
            Values.THEME_MODE_DARK -> preferences.themeDark
            else -> {
                val mode = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                if (mode.nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
                    preferences.themeDark
                } else {
                    R.style.Theme_Tachiyomi
                }
            }
        })
        super.onCreate(savedInstanceState)
    }

}
