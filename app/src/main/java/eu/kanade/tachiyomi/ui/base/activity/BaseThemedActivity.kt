package eu.kanade.tachiyomi.ui.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues.DarkThemeVariant
import eu.kanade.tachiyomi.data.preference.PreferenceValues.LightThemeVariant
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.util.view.setSecureScreen
import kotlinx.coroutines.flow.launchIn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

abstract class BaseThemedActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeResourceId(preferences))

        Injekt.get<PreferencesHelper>().incognitoMode()
            .asImmediateFlow {
                window.setSecureScreen(it)
            }
            .launchIn(lifecycleScope)

        super.onCreate(savedInstanceState)
    }

    companion object {
        fun getThemeResourceId(preferences: PreferencesHelper): Int {
            return if (preferences.isDarkMode()) {
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
                    LightThemeVariant.yotsuba -> R.style.Theme_Tachiyomi_Light_Yotsuba
                }
            }
        }
    }
}
