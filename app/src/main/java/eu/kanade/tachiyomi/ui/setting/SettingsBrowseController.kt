package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.ExtensionUpdateJob
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsBrowseController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.browse

        preferenceCategory {
            titleRes = R.string.label_extensions

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
        }

        preferenceCategory {
            titleRes = R.string.action_global_search

            switchPreference {
                key = Keys.searchPinnedSourcesOnly
                titleRes = R.string.pref_search_pinned_sources_only
                defaultValue = false
            }
        }
    }
}
