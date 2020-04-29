package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.extension.ExtensionUpdateJob
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes

class SettingsBrowseController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.browse

        switchPreference {
            key = Keys.automaticExtUpdates
            titleRes = R.string.pref_enable_automatic_extension_updates
            defaultValue = true

            onChange { newValue ->
                val checked = newValue as Boolean
                ExtensionUpdateJob.setupTask(activity!!, checked)
                true
            }
        }
        switchPreference {
            key = Keys.searchPinnedSourcesOnly
            titleRes = R.string.pref_search_pinned_sources_only
            defaultValue = false
        }
    }
}
