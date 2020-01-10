package eu.kanade.tachiyomi.ui.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.LocaleHelper
import uy.kohesive.injekt.injectLazy

abstract class BaseActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(when (preferences.theme()) {
            2 -> R.style.Theme_Tachiyomi_Dark
            3 -> R.style.Theme_Tachiyomi_Amoled
            4 -> R.style.Theme_Tachiyomi_DarkBlue
            else -> R.style.Theme_Tachiyomi
        })
        super.onCreate(savedInstanceState)
    }

}
