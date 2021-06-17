package eu.kanade.tachiyomi.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import eu.kanade.tachiyomi.ui.setting.SettingsBackupController
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
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class MoreController :
    SettingsController(),
    RootController,
    NoToolbarElevationController {

    private val downloadManager: DownloadManager by injectLazy()
    private var isDownloading: Boolean = false
    private var downloadQueueSize: Int = 0

    private var untilDestroySubscriptions = CompositeSubscription()
        private set

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
            iconRes = R.drawable.ic_glasses_24dp
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
                titleRes = R.string.categories
                iconRes = R.drawable.ic_label_24dp
                iconTint = tintColor
                onClick {
                    router.pushController(CategoryController().withFadeTransaction())
                }
            }
            preference {
                titleRes = R.string.label_backup
                iconRes = R.drawable.ic_settings_backup_restore_24dp
                iconTint = tintColor
                onClick {
                    router.pushController(SettingsBackupController().withFadeTransaction())
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        if (untilDestroySubscriptions.isUnsubscribed) {
            untilDestroySubscriptions = CompositeSubscription()
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        untilDestroySubscriptions.unsubscribe()
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

    private fun <T> Observable<T>.subscribeUntilDestroy(onNext: (T) -> Unit): Subscription {
        return subscribe(onNext).also { untilDestroySubscriptions.add(it) }
    }

    companion object {
        const val URL_HELP = "https://tachiyomi.org/help/"
    }
}
