package eu.kanade.tachiyomi.ui.more

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.ui.base.controller.NoToolbarElevationController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.download.DownloadController
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.ui.setting.SettingsMainController
import eu.kanade.tachiyomi.util.preference.add
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.iconRes
import eu.kanade.tachiyomi.util.preference.iconTint
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class MoreController :
    SettingsController(),
    RootController,
    NoToolbarElevationController {

    private val downloadManager: DownloadManager by injectLazy()
    private var isDownloading: Boolean = false
    private var downloadQueueSize: Int = 0

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.label_more

        val tintColor = context.getResourceColor(R.attr.colorAccent)

        add(MoreHeaderPreference(context))

        switchPreference {
            key = Keys.downloadedOnly
            titleRes = R.string.label_downloaded_only
            summaryRes = R.string.downloaded_only_summary
            iconRes = R.drawable.ic_cloud_off_24dp
            iconTint = tintColor
        }

        switchPreference {
            key = Keys.incognitoMode
            summaryRes = R.string.pref_incognito_mode_summary
            titleRes = R.string.pref_incognito_mode
            iconRes = R.drawable.ic_glasses_black_24dp
            iconTint = tintColor
            defaultValue = false
        }

        preferenceCategory {
            preference {
                titleRes = R.string.label_download_queue

                if (downloadManager.queue.isNotEmpty()) {
                    initDownloadQueueSummary(this)
                }

                iconRes = R.drawable.ic_get_app_24dp
                iconTint = tintColor
                onClick {
                    router.pushController(DownloadController().withFadeTransaction())
                }
            }
            preference {
                titleRes = R.string.label_categories
                iconRes = R.drawable.ic_label_24dp
                iconTint = tintColor
                onClick {
                    router.pushController(CategoryController().withFadeTransaction())
                }
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
                iconRes = R.drawable.ic_help_24dp
                iconTint = tintColor
                onClick {
                    activity?.openInBrowser(URL_HELP)
                }
            }
        }
    }

    private fun initDownloadQueueSummary(preference: Preference) {
        // Handle running/paused status change
        DownloadService.runningRelay
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { isRunning ->
                isDownloading = isRunning
                updateDownloadQueueSummary(preference)
            }

        // Handle queue progress updating
        downloadManager.queue.getUpdatedObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy {
                downloadQueueSize = it.size
                updateDownloadQueueSummary(preference)
            }
    }

    private fun updateDownloadQueueSummary(preference: Preference) {
        preference.summary = when {
            downloadQueueSize == 0 -> null
            !isDownloading -> resources?.getString(R.string.paused)
            else -> resources?.getQuantityString(R.plurals.download_queue_summary, downloadQueueSize, downloadQueueSize)
        }
    }

    private class MoreHeaderPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Preference(context, attrs) {

        init {
            layoutResource = R.layout.pref_more_header
            isSelectable = false
        }
    }

    companion object {
        private const val URL_HELP = "https://tachiyomi.org/help/"
    }
}
