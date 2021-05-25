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
                DarkThemeVariant.greenapple -> R.style.Theme_Tachiyomi_Dark_GreenApple
                DarkThemeVariant.midnightdusk -> R.style.Theme_Tachiyomi_Dark_MidnightDusk
                DarkThemeVariant.amoled -> R.style.Theme_Tachiyomi_Amoled
                DarkThemeVariant.hotpink -> R.style.Theme_Tachiyomi_Amoled_HotPink
            }
        } else {
            when (preferences.themeLight().get()) {
                LightThemeVariant.default -> R.style.Theme_Tachiyomi_Light
                LightThemeVariant.blue -> R.style.Theme_Tachiyomi_Light_Blue
                LightThemeVariant.strawberrydaiquiri -> R.style.Theme_Tachiyomi_Light_StrawberryDaiquiri
            }
        }
        setTheme(themeId)
        super.onCreate(savedInstanceState)
    }
}
