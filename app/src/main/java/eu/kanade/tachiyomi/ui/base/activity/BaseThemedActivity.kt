package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.view.setSecureScreen
import kotlinx.coroutines.flow.launchIn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

abstract class BaseThemedActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.createLocaleWrapper(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemePreferences(preferences)

        Injekt.get<PreferencesHelper>().incognitoMode()
            .asImmediateFlow {
                window.setSecureScreen(it)
            }
            .launchIn(lifecycleScope)

        super.onCreate(savedInstanceState)
    }

    companion object {
        fun AppCompatActivity.applyThemePreferences(preferences: PreferencesHelper) {
            val resIds = mutableListOf<Int>()
            when (preferences.appTheme().get()) {
                PreferenceValues.AppTheme.BLUE -> {
                    resIds += R.style.Theme_Tachiyomi_Blue
                    resIds += R.style.ThemeOverlay_Tachiyomi_ColoredBars
                }
                PreferenceValues.AppTheme.GREEN_APPLE -> {
                    resIds += R.style.Theme_Tachiyomi_GreenApple
                }
                PreferenceValues.AppTheme.MIDNIGHT_DUSK -> {
                    resIds += R.style.Theme_Tachiyomi_MidnightDusk
                }
                PreferenceValues.AppTheme.STRAWBERRY_DAIQUIRI -> {
                    resIds += R.style.Theme_Tachiyomi_StrawberryDaiquiri
                }
                PreferenceValues.AppTheme.YOTSUBA -> {
                    resIds += R.style.Theme_Tachiyomi_Yotsuba
                }
                else -> {
                    resIds += R.style.Theme_Tachiyomi
                }
            }

            if (preferences.themeDarkAmoled().get()) {
                resIds += R.style.ThemeOverlay_Tachiyomi_Amoled
            }

            resIds.forEach {
                setTheme(it)
            }

            lifecycleScope.launchWhenCreated {
                AppCompatDelegate.setDefaultNightMode(
                    when (preferences.themeMode().get()) {
                        PreferenceValues.ThemeMode.light -> AppCompatDelegate.MODE_NIGHT_NO
                        PreferenceValues.ThemeMode.dark -> AppCompatDelegate.MODE_NIGHT_YES
                        PreferenceValues.ThemeMode.system -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
            }
        }
    }
}
