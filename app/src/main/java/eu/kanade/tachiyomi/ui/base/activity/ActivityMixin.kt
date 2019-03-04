package eu.kanade.tachiyomi.ui.base.activity

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface ActivityMixin {
    fun setAppTheme() {
        setTheme(when (Injekt.get<PreferencesHelper>().theme()) {
            2 -> R.style.Theme_Tachiyomi_Dark
            3 -> R.style.Theme_Tachiyomi_Amoled
            4 -> R.style.Theme_Tachiyomi_DarkBlue
            else -> R.style.Theme_Tachiyomi
        })
    }
    
    fun setTheme(resource: Int)
}
