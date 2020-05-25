package eu.kanade.tachiyomi.ui.base.activity

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.LocaleHelper
import uy.kohesive.injekt.injectLazy

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    val scope = lifecycleScope
    lateinit var binding: VB

    @Suppress("LeakingThis")
    private val secureActivityDelegate = SecureActivityDelegate(this)

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

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(
            when (preferences.themeMode().get()) {
                Values.ThemeMode.system -> {
                    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                        darkTheme
                    } else {
                        lightTheme
                    }
                }
                Values.ThemeMode.dark -> darkTheme
                else -> lightTheme
            }
        )

        super.onCreate(savedInstanceState)

        secureActivityDelegate.onCreate()
    }

    override fun onResume() {
        super.onResume()

        secureActivityDelegate.onResume()
    }
}
