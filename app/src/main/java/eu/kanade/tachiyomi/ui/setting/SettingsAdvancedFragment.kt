package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.v7.preference.Preference
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.util.toast
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicInteger

class SettingsAdvancedFragment : SettingsNestedFragment() {

    private var clearCacheSubscription: Subscription? = null

    companion object {

        fun newInstance(resourcePreference: Int, resourceTitle: Int): SettingsNestedFragment {
            val fragment = SettingsAdvancedFragment()
            fragment.setArgs(resourcePreference, resourceTitle)
            return fragment
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val clearCache = findPreference(getString(R.string.pref_clear_chapter_cache_key))
        val clearDatabase = findPreference(getString(R.string.pref_clear_database_key))

        clearCache.setOnPreferenceClickListener { preference ->
            clearChapterCache(preference)
            true
        }
        clearCache.summary = getString(R.string.used_cache, chapterCache.readableSize)

        clearDatabase.setOnPreferenceClickListener { preference ->
            clearDatabase()
            true
        }
    }

    override fun onDestroyView() {
        clearCacheSubscription?.unsubscribe()
        super.onDestroyView()
    }

    private fun clearChapterCache(preference: Preference) {
        val deletedFiles = AtomicInteger()

        val files = chapterCache.cacheDir.listFiles()

        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.deleting)
                .progress(false, files.size, true)
                .cancelable(false)
                .show()

        clearCacheSubscription?.unsubscribe()

        clearCacheSubscription = Observable.defer { Observable.from(files) }
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
                    context.toast(R.string.cache_delete_error)
                }, {
                    dialog.dismiss()
                    context.toast(getString(R.string.cache_deleted, deletedFiles.get()))
                    preference.summary = getString(R.string.used_cache, chapterCache.readableSize)
                })
    }

    private fun clearDatabase() {
        MaterialDialog.Builder(activity)
                .content(R.string.clear_database_confirmation)
                .positiveText(R.string.button_yes)
                .negativeText(R.string.button_no)
                .onPositive { dialog, which -> db.deleteMangasNotInLibrary().executeAsBlocking() }
                .show()
    }

    private val chapterCache: ChapterCache
        get() = settingsActivity.chapterCache

    private val db: DatabaseHelper
        get() = settingsActivity.db

}
