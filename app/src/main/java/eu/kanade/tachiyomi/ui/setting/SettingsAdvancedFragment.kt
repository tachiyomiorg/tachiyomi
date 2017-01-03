package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.util.toast
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.atomic.AtomicInteger

class SettingsAdvancedFragment : SettingsFragment() {

    companion object {
        fun newInstance(rootKey: String): SettingsAdvancedFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsAdvancedFragment().apply { arguments = args }
        }
    }

    private val network: NetworkHelper by injectLazy()

    private val chapterCache: ChapterCache by injectLazy()

    private val db: DatabaseHelper by injectLazy()

    private val clearCache: Preference by bindPref(R.string.pref_clear_chapter_cache_key)

    private val clearDatabase: Preference by bindPref(R.string.pref_clear_database_key)

    private val clearCookies: Preference by bindPref(R.string.pref_clear_cookies_key)

    private val refreshMetadata: Preference by bindPref(R.string.pref_refresh_library_metadata_key)

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        clearCache.setOnPreferenceClickListener {
            clearChapterCache()
            true
        }
        clearCache.summary = getString(R.string.used_cache, chapterCache.readableSize)

        clearCookies.setOnPreferenceClickListener {
            network.cookies.removeAll()
            activity.toast(R.string.cookies_cleared)
            true
        }

        clearDatabase.setOnPreferenceClickListener {
            clearDatabase()
            true
        }

        refreshMetadata.setOnPreferenceClickListener {
            LibraryUpdateService.start(context, details = true)
            true
        }
    }

    private fun clearChapterCache() {
        val deletedFiles = AtomicInteger()

        val files = chapterCache.cacheDir.listFiles() ?: return

        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.deleting)
                .progress(false, files.size, true)
                .cancelable(false)
                .show()

        subscriptions += Observable.defer { Observable.from(files) }
                .concatMap { file ->
                    if (chapterCache.removeFileFromCache(file.name)) {
                        deletedFiles.incrementAndGet()
                    }
                    Observable.just(file)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    dialog.incrementProgress(1)
                }, {
                    dialog.dismiss()
                    activity.toast(R.string.cache_delete_error)
                }, {
                    dialog.dismiss()
                    activity.toast(getString(R.string.cache_deleted, deletedFiles.get()))
                    clearCache.summary = getString(R.string.used_cache, chapterCache.readableSize)
                })
    }

    private fun clearDatabase() {
        MaterialDialog.Builder(activity)
                .content(R.string.clear_database_confirmation)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive { dialog, which ->
                    (activity as SettingsActivity).parentFlags = SettingsActivity.FLAG_DATABASE_CLEARED
                    db.deleteMangasNotInLibrary().executeAsBlocking()
                    activity.toast(R.string.clear_database_completed)
                }
                .show()
    }

}
