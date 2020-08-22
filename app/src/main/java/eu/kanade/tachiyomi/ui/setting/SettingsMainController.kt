package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.util.preference.iconRes
import eu.kanade.tachiyomi.util.preference.iconTint
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.getResourceColor

class SettingsMainController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.label_settings

        val tintColor = context.getResourceColor(R.attr.colorAccent)

        preference {
            iconRes = R.drawable.ic_tune_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_general
            onClick { navigateTo(SettingsGeneralController()) }
        }
        preference {
            iconRes = R.drawable.ic_collections_bookmark_outline_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_library
            onClick { navigateTo(SettingsLibraryController()) }
        }
        preference {
            iconRes = R.drawable.ic_chrome_reader_mode_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_reader
            onClick { navigateTo(SettingsReaderController()) }
        }
        preference {
            iconRes = R.drawable.ic_get_app_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_downloads
            onClick { navigateTo(SettingsDownloadController()) }
        }
        preference {
            iconRes = R.drawable.ic_sync_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_tracking
            onClick { navigateTo(SettingsTrackingController()) }
        }
        preference {
            iconRes = R.drawable.ic_explore_outline_24dp
            iconTint = tintColor
            titleRes = R.string.browse
            onClick { navigateTo(SettingsBrowseController()) }
        }
        preference {
            iconRes = R.drawable.ic_backup_24dp
            iconTint = tintColor
            titleRes = R.string.backup
            onClick { navigateTo(SettingsBackupController()) }
        }
        preference {
            iconRes = R.drawable.ic_security_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_security
            onClick { navigateTo(SettingsSecurityController()) }
        }
        preference {
            iconRes = R.drawable.ic_outline_people_alt_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_parental_controls
            onClick { navigateTo(SettingsParentalControlsController()) }
        }
        preference {
            iconRes = R.drawable.ic_code_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_advanced
            onClick { navigateTo(SettingsAdvancedController()) }
        }
    }

    private fun navigateTo(controller: SettingsController) {
        router.pushController(controller.withFadeTransaction())
    }
}
