package eu.kanade.tachiyomi.ui.base.activity

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues.DarkThemeVariant
import eu.kanade.tachiyomi.data.preference.PreferenceValues.LightThemeVariant
import eu.kanade.tachiyomi.data.preference.PreferenceValues.ThemeMode
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy

abstract class BaseThemedActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDarkMode = when (preferences.themeMode().get()) {
            ThemeMode.light -> false
            ThemeMode.dark -> true
            ThemeMode.system -> resources.configuration.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
        }
        val themeId = if (isDarkMode) {
            when (preferences.themeDark().get()) {
                DarkThemeVariant.default -> R.style.Theme_Tachiyomi_Dark
                DarkThemeVariant.blue -> R.style.Theme_Tachiyomi_Dark_Blue
                DarkThemeVariant.amoled -> R.style.Theme_Tachiyomi_Dark_Amoled
            }
        } else {
            when (preferences.themeLight().get()) {
                LightThemeVariant.default -> R.style.Theme_Tachiyomi_Light
                LightThemeVariant.blue -> R.style.Theme_Tachiyomi_Light_Blue
            }
        }
        setTheme(themeId)
        super.onCreate(savedInstanceState)
    }
}
