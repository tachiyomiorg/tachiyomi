package eu.kanade.tachiyomi.ui.base.activity

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values

abstract class BaseThemedActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    private val isDarkMode: Boolean by lazy {
        val themeMode = preferences.themeMode().get()
        (themeMode == Values.ThemeMode.dark) ||
            (
                themeMode == Values.ThemeMode.system &&
                    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
                )
    }

    private val lightTheme: Int by lazy {
        when (preferences.themeLight().get()) {
            Values.LightThemeVariant.blue -> R.style.Theme_Tachiyomi_LightBlue
            else -> {
                when {
                    // Light status + navigation bar
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
                        R.style.Theme_Tachiyomi_Light_Api27
                    }
                    // Light status bar + fallback gray navigation bar
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        R.style.Theme_Tachiyomi_Light_Api23
                    }
                    // Fallback gray status + navigation bar
                    else -> {
                        R.style.Theme_Tachiyomi_Light
                    }
                }
            }
        }
    }

    private val darkTheme: Int by lazy {
        when (preferences.themeDark().get()) {
            Values.DarkThemeVariant.blue -> R.style.Theme_Tachiyomi_DarkBlue
            Values.DarkThemeVariant.amoled -> R.style.Theme_Tachiyomi_Amoled
            else -> R.style.Theme_Tachiyomi_Dark
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(
            when {
                isDarkMode -> darkTheme
                else -> lightTheme
            }
        )

        super.onCreate(savedInstanceState)
    }
}
