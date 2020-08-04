package eu.kanade.tachiyomi.ui.browse.migration.search

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.browse.migration.MigrationFlags
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchController
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchPresenter
import uy.kohesive.injekt.injectLazy

class SearchController(
    private var manga: Manga? = null
) : GlobalSearchController(manga?.title) {

    private var newManga: Manga? = null

    override fun createPresenter(): GlobalSearchPresenter {
        return SearchPresenter(
            initialQuery,
            manga!!
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(::manga.name, manga)
        outState.putSerializable(::newManga.name, newManga)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        manga = savedInstanceState.getSerializable(::manga.name) as? Manga
        newManga = savedInstanceState.getSerializable(::newManga.name) as? Manga
    }

    fun migrateManga() {
        val manga = manga ?: return
        val newManga = newManga ?: return

        (presenter as? SearchPresenter)?.migrateManga(manga, newManga, true)
    }

    fun copyManga() {
        val manga = manga ?: return
        val newManga = newManga ?: return

        (presenter as? SearchPresenter)?.migrateManga(manga, newManga, false)
    }

    override fun onMangaClick(manga: Manga) {
        newManga = manga
        val dialog =
            MigrationDialog()
        dialog.targetController = this
        dialog.showDialog(router)
    }

    override fun onMangaLongClick(manga: Manga) {
        // Call parent's default click listener
        super.onMangaClick(manga)
    }

    fun renderIsReplacingManga(isReplacingManga: Boolean) {
        if (isReplacingManga) {
            binding.progress.isVisible = true
        } else {
            binding.progress.isVisible = false
            router.popController(this)
        }
    }

    class MigrationDialog : DialogController() {

        private val preferences: PreferencesHelper by injectLazy()

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val prefValue = preferences.migrateFlags().get()

            val preselected =
                MigrationFlags.getEnabledFlagsPositions(
                    prefValue
                )

            return MaterialDialog(activity!!)
                .message(R.string.migration_dialog_what_to_include)
                .listItemsMultiChoice(
                    items = MigrationFlags.titles.map { resources?.getString(it) as CharSequence },
                    initialSelection = preselected.toIntArray()
                ) { _, positions, _ ->
                    // Save current settings for the next time
                    val newValue =
                        MigrationFlags.getFlagsFromPositions(
                            positions.toTypedArray()
                        )
                    preferences.migrateFlags().set(newValue)
                }
                .positiveButton(R.string.migrate) {
                    (targetController as? SearchController)?.migrateManga()
                }
                .negativeButton(R.string.copy) {
                    (targetController as? SearchController)?.copyManga()
                }
                .neutralButton(android.R.string.cancel)
        }
    }
}
