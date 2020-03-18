package eu.kanade.tachiyomi.ui.more

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.download.DownloadController
import eu.kanade.tachiyomi.ui.extension.ExtensionController
import eu.kanade.tachiyomi.ui.migration.MigrationController
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.ui.setting.SettingsMainController
import eu.kanade.tachiyomi.util.preference.extensionPreference
import eu.kanade.tachiyomi.util.preference.iconRes
import eu.kanade.tachiyomi.util.preference.iconTint
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser

class MoreController : SettingsController(), RootController {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.label_more

        val tintColor = context.getResourceColor(R.attr.colorAccent)

        extensionPreference {
            titleRes = R.string.label_extensions
            iconRes = R.drawable.ic_extension_24dp
            iconTint = tintColor
            onClick {
                router.pushController(ExtensionController().withFadeTransaction())
            }
        }
        preference {
            titleRes = R.string.label_download_queue
            iconRes = R.drawable.ic_file_download_black_24dp
            iconTint = tintColor
            onClick {
                router.pushController(DownloadController().withFadeTransaction())
            }
        }
        preference {
            titleRes = R.string.label_migration
            iconRes = R.drawable.ic_compare_arrows_black_24dp
            iconTint = tintColor
            onClick {
                router.pushController(MigrationController().withFadeTransaction())
            }
        }

        preferenceCategory {
            preference {
                titleRes = R.string.label_settings
                iconRes = R.drawable.ic_settings_24dp
                iconTint = tintColor
                onClick {
                    router.pushController(SettingsMainController().withFadeTransaction())
                }
            }
            preference {
                iconRes = R.drawable.ic_info_24dp
                iconTint = tintColor
                titleRes = R.string.pref_category_about
                onClick {
                    router.pushController(AboutController().withFadeTransaction())
                }
            }
            preference {
                titleRes = R.string.label_help
                iconRes = R.drawable.ic_help_outline_24dp
                iconTint = tintColor
                onClick {
                    activity?.openInBrowser(URL_HELP)
                }
            }
        }
    }

    companion object {
        private const val URL_HELP = "https://tachiyomi.org/help/"
    }
}
