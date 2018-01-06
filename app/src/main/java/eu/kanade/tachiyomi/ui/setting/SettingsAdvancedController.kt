package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.os.Bundle
import android.support.v7.preference.PreferenceScreen
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.library.LibraryUpdateService.Target
import eu.kanade.tachiyomi.data.sync.LibrarySyncManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.util.toast
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsAdvancedController : SettingsController() {
    private val syncManager: LibrarySyncManager by injectLazy()

    private val network: NetworkHelper by injectLazy()

    private val chapterCache: ChapterCache by injectLazy()

    private val db: DatabaseHelper by injectLazy()
    
    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_advanced

        preference {
            key = CLEAR_CACHE_KEY
            titleRes = R.string.pref_clear_chapter_cache
            summary = context.getString(R.string.used_cache, chapterCache.readableSize)

            onClick { clearChapterCache() }
        }
        preference {
            titleRes = R.string.pref_clear_cookies

            onClick {
                network.cookies.removeAll()
                activity?.toast(R.string.cookies_cleared)
            }
        }
        preference {
            key = CLEAR_DATABASE_KEY
            titleRes = R.string.pref_clear_database
            summaryRes = R.string.pref_clear_database_summary

            onClick {
                val ctrl = ClearDatabaseDialogController(syncManager)
                ctrl.targetController = this@SettingsAdvancedController
                ctrl.showDialog(router)
            }
        }
        preference {
            titleRes = R.string.pref_refresh_library_metadata
            summaryRes = R.string.pref_refresh_library_metadata_summary

            onClick { LibraryUpdateService.start(context, target = Target.DETAILS) }
        }
        preference {
            titleRes = R.string.pref_refresh_library_tracking
            summaryRes = R.string.pref_refresh_library_tracking_summary

            onClick { LibraryUpdateService.start(context, target = Target.TRACKING) }
        }
    }

    private fun clearChapterCache() {
        if (activity == null) return
        val files = chapterCache.cacheDir.listFiles() ?: return

        var deletedFiles = 0

        val ctrl = DeletingFilesDialogController()
        ctrl.total = files.size
        ctrl.showDialog(router)

        Observable.defer { Observable.from(files) }
                .doOnNext { file ->
                    if (chapterCache.removeFileFromCache(file.name)) {
                        deletedFiles++
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    ctrl.setProgress(deletedFiles)
                }, {
                    activity?.toast(R.string.cache_delete_error)
                }, {
                    ctrl.finish()
                    activity?.toast(resources?.getString(R.string.cache_deleted, deletedFiles))
                    findPreference(CLEAR_CACHE_KEY)?.summary =
                            resources?.getString(R.string.used_cache, chapterCache.readableSize)
                })
    }

    class DeletingFilesDialogController : DialogController() {

        var total = 0

        private var materialDialog: MaterialDialog? = null

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            return MaterialDialog.Builder(activity!!)
                    .title(R.string.deleting)
                    .progress(false, total, true)
                    .cancelable(false)
                    .build()
                    .also { materialDialog = it }
        }

        override fun onDestroyView(view: View) {
            super.onDestroyView(view)
            materialDialog = null
        }

        override fun onRestoreInstanceState(savedInstanceState: Bundle) {
            super.onRestoreInstanceState(savedInstanceState)
            finish()
        }

        fun setProgress(deletedFiles: Int) {
            materialDialog?.setProgress(deletedFiles)
        }

        fun finish() {
            router.popController(this)
        }
    }

    class ClearDatabaseDialogController(private val syncManager: LibrarySyncManager = Injekt.get()):
            DialogController() {
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            //Do not allow clearing database while sync is enabled
            return if(syncManager.account != null)
                MaterialDialog.Builder(activity!!)
                        .content(R.string.pref_clear_database_sync_error)
                        .positiveText(android.R.string.ok)
                        .build()
            else
                MaterialDialog.Builder(activity!!)
                    .content(R.string.clear_database_confirmation)
                    .positiveText(android.R.string.yes)
                    .negativeText(android.R.string.no)
                    .onPositive { _, _ ->
                        (targetController as? SettingsAdvancedController)?.clearDatabase()
                    }
                    .build()
        }
    }

    private fun clearDatabase() {
        // Avoid weird behavior by going back to the library.
        val newBackstack = listOf(RouterTransaction.with(LibraryController())) +
                router.backstack.drop(1)

        router.setBackstack(newBackstack, FadeChangeHandler())

        // Check that sync is not active again, as user could have enabled sync
        // after opening the clear database confirmation dialog
        if(syncManager.account != null) {
            activity?.toast(R.string.pref_clear_database_sync_error)
        } else {
            db.deleteMangasNotInLibrary().executeAsBlocking()
            db.deleteHistoryNoLastRead().executeAsBlocking()
            activity?.toast(R.string.clear_database_completed)
        }
    }

    private companion object {
        const val CLEAR_DATABASE_KEY = "pref_clear_database_key"
        const val CLEAR_CACHE_KEY = "pref_clear_cache_key"
    }
}
