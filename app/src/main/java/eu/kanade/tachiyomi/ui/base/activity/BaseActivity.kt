package eu.kanade.tachiyomi.ui.base.activity

import android.content.res.Configuration
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

    private val darkTheme: Int by lazy {
        when (preferences.themeDark()) {
            Values.THEME_DARK_DEFAULT -> R.style.Theme_Tachiyomi_Dark
            Values.THEME_DARK_AMOLED -> R.style.Theme_Tachiyomi_Amoled
            else -> R.style.Theme_Tachiyomi_DarkBlue
        }
    }

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(when (preferences.themeMode().getOrDefault()) {
            Values.THEME_MODE_DARK -> darkTheme
            Values.THEME_MODE_SYSTEM -> {
                if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    darkTheme
                } else {
                    R.style.Theme_Tachiyomi
                }
            }
            else -> R.style.Theme_Tachiyomi
        })

        super.onCreate(savedInstanceState)

        SecureActivityDelegate.onCreate(this)
    }

    override fun onResume() {
        super.onResume()

        SecureActivityDelegate.onResume(this)
    }
}
