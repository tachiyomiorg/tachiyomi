package eu.kanade.tachiyomi.ui.base.activity

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.LocaleHelper
import uy.kohesive.injekt.injectLazy

abstract class BaseActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    @Suppress("LeakingThis")
    private val secureActivityDelegate = SecureActivityDelegate(this)

    private val lightTheme: Int by lazy {
        when (preferences.themeLight()) {
            Values.THEME_LIGHT_BLUE -> R.style.Theme_Tachiyomi_LightBlue
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
        when (preferences.themeDark()) {
            Values.THEME_DARK_BLUE -> R.style.Theme_Tachiyomi_DarkBlue
            Values.THEME_DARK_AMOLED -> R.style.Theme_Tachiyomi_Amoled
            else -> R.style.Theme_Tachiyomi_Dark
        }
    }

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(when (preferences.themeMode().getOrDefault()) {
            Values.THEME_MODE_SYSTEM -> {
                if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    darkTheme
                } else {
                    lightTheme
                }
            }
            Values.THEME_MODE_DARK -> darkTheme
            else -> lightTheme
        })

        super.onCreate(savedInstanceState)

        secureActivityDelegate.onCreate()
    }

    override fun onResume() {
        super.onResume()

        secureActivityDelegate.onResume()
    }
}
