package eu.kanade.tachiyomi.ui.extension

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.ui.setting.onChange
import eu.kanade.tachiyomi.ui.setting.switchPreference
import eu.kanade.tachiyomi.ui.setting.titleRes
import eu.kanade.tachiyomi.util.LocaleHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionFilterController: SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.action_filter

        val activeLangs = preferences.enabledLanguages().getOrDefault()

        val availableLangs =
                Injekt.get<ExtensionManager>().availableExtensions.groupBy {
                    it.lang
                }.keys.minus("all").partition {
                    it in activeLangs
                }.let {
                    it.first + it.second
                }

        availableLangs.forEach {
            switchPreference {
                preferenceScreen.addPreference(this)
                title = LocaleHelper.getDisplayName(it, context)
                isPersistent = false
                isChecked = it in activeLangs

                onChange { newValue ->
                    val checked = newValue as Boolean
                    val currentActiveLangs = preferences.enabledLanguages().getOrDefault()

                    if (checked) {
                        preferences.enabledLanguages().set(currentActiveLangs + it)
                    } else {
                        preferences.enabledLanguages().set(currentActiveLangs - it)
                    }
                    true
                }
            }
        }
    }
}
