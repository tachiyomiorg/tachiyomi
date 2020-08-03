package eu.kanade.tachiyomi.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.net.toUri
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.library.LibraryUpdateService.Target
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.powerManager
import eu.kanade.tachiyomi.util.system.toast
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy

class SettingsAdvancedController : SettingsController() {

    private val network: NetworkHelper by injectLazy()

    private val chapterCache: ChapterCache by injectLazy()

    private val db: DatabaseHelper by injectLazy()

    @SuppressLint("BatteryLife")
    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_advanced

        switchPreference {
            key = "acra.enable"
            titleRes = R.string.pref_enable_acra
            summaryRes = R.string.pref_acra_summary
            defaultValue = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            preference {
                titleRes = R.string.pref_disable_battery_optimization
                summaryRes = R.string.pref_disable_battery_optimization_summary

                onClick {
                    val packageName: String = context.packageName
                    if (!context.powerManager.isIgnoringBatteryOptimizations(packageName)) {
                        try {
                            val intent = Intent().apply {
                                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                data = "package:$packageName".toUri()
                            }
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            context.toast(R.string.battery_optimization_setting_activity_not_found)
                        }
                    } else {
                        context.toast(R.string.battery_optimization_disabled)
                    }
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.label_data

            preference {
                key = CLEAR_CACHE_KEY
                titleRes = R.string.pref_clear_chapter_cache
                summary = context.getString(R.string.used_cache, chapterCache.readableSize)

                onClick { clearChapterCache() }
            }
            preference {
                titleRes = R.string.pref_clear_database
                summaryRes = R.string.pref_clear_database_summary

                onClick {
                    val ctrl = ClearDatabaseDialogController()
                    ctrl.targetController = this@SettingsAdvancedController
                    ctrl.showDialog(router)
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.label_network

            preference {
                titleRes = R.string.pref_clear_cookies

                onClick {
                    network.cookieManager.removeAll()
                    activity?.toast(R.string.cookies_cleared)
                }
            }
            switchPreference {
                key = Keys.enableDoh
                titleRes = R.string.pref_dns_over_https
                summaryRes = R.string.pref_dns_over_https_summary
                defaultValue = false
            }
        }

        preferenceCategory {
            titleRes = R.string.label_library

            preference {
                titleRes = R.string.pref_refresh_library_covers

                onClick { LibraryUpdateService.start(context, target = Target.COVERS) }
            }
            preference {
                titleRes = R.string.pref_refresh_library_tracking
                summaryRes = R.string.pref_refresh_library_tracking_summary

                onClick { LibraryUpdateService.start(context, target = Target.TRACKING) }
            }
        }
    }

    private fun clearChapterCache() {
        if (activity == null) return
        val files = chapterCache.cacheDir.listFiles() ?: return

        var deletedFiles = 0

        Observable.defer { Observable.from(files) }
            .doOnNext { file ->
                if (chapterCache.removeFileFromCache(file.name)) {
                    deletedFiles++
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                activity?.toast(R.string.cache_delete_error)
            }
            .doOnCompleted {
                activity?.toast(resources?.getString(R.string.cache_deleted, deletedFiles))
                findPreference(CLEAR_CACHE_KEY)?.summary =
                    resources?.getString(R.string.used_cache, chapterCache.readableSize)
            }
            .subscribe()
    }

    class ClearDatabaseDialogController : DialogController() {
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            return MaterialDialog(activity!!)
                .message(R.string.clear_database_confirmation)
                .positiveButton(android.R.string.ok) {
                    (targetController as? SettingsAdvancedController)?.clearDatabase()
                }
                .negativeButton(android.R.string.cancel)
        }
    }

    private fun clearDatabase() {
        db.deleteMangasNotInLibrary().executeAsBlocking()
        db.deleteHistoryNoLastRead().executeAsBlocking()
        activity?.toast(R.string.clear_database_completed)
    }

    private companion object {
        const val CLEAR_CACHE_KEY = "pref_clear_cache_key"
    }
}
