package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.infoPreference
import eu.kanade.tachiyomi.util.preference.listPreference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.titleRes

class SettingsParentalControlsController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_parental_controls

        listPreference {
            key = Keys.allowNsfwSource
            titleRes = R.string.pref_allow_nsfw_sources
            entriesRes = arrayOf(
                R.string.pref_allow_nsfw_sources_allowed,
                R.string.pref_allow_nsfw_sources_allowed_multisource,
                R.string.pref_allow_nsfw_sources_blocked
            )
            entryValues = arrayOf(
                Values.NsfwAllowance.ALLOWED.name,
                Values.NsfwAllowance.PARTIAL.name,
                Values.NsfwAllowance.BLOCKED.name
            )
            defaultValue = Values.NsfwAllowance.ALLOWED.name
            summary = "%s"
        }

        preferenceCategory {
            infoPreference(R.string.parental_controls_info)
        }
    }
}
