package eu.kanade.tachiyomi.ui.browse.migration.search

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.migration.MigrationFlags
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchController
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchPresenter
import eu.kanade.tachiyomi.ui.manga.MangaController
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

    fun migrateManga(manga: Manga? = null, newManga: Manga?) {
        manga ?: return
        newManga ?: return

        (presenter as? SearchPresenter)?.migrateManga(manga, newManga, true)
    }

    fun copyManga(manga: Manga? = null, newManga: Manga?) {
        manga ?: return
        newManga ?: return

        (presenter as? SearchPresenter)?.migrateManga(manga, newManga, false)
    }

    override fun onMangaClick(manga: Manga) {
        newManga = manga
        val dialog =
            MigrationDialog(this.manga, newManga, this)
        dialog.targetController = this
        dialog.showDialog(router)
    }

    override fun onMangaLongClick(manga: Manga) {
        // Call parent's default click listener
        super.onMangaClick(manga)
    }

    fun renderIsReplacingManga(isReplacingManga: Boolean, newManga: Manga?) {
        binding.progress.isVisible = isReplacingManga
        if (!isReplacingManga) {
            router.popController(this)
            if (newManga != null) {
                // Replaces old MangaController
                router.replaceTopController(RouterTransaction.with(MangaController(newManga)))
            }
        }
    }

    class MigrationDialog(private val manga: Manga? = null, private val newManga: Manga? = null, private val callingController: Controller? = null) : DialogController() {

        private val preferences: PreferencesHelper by injectLazy()

        @Suppress("DEPRECATION")
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val prefValue = preferences.migrateFlags().get()

            val preselected =
                MigrationFlags.getEnabledFlagsPositions(
                    prefValue
                )

            return MaterialDialog(activity!!)
                .title(R.string.migration_dialog_what_to_include)
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
                    if (callingController != null) {
                        if (callingController.javaClass == SourceSearchController::class.java) {
                            router.popController(callingController)
                        }
                    }
                    (targetController as? SearchController)?.migrateManga(manga, newManga)
                }
                .negativeButton(R.string.copy) {
                    if (callingController != null) {
                        if (callingController.javaClass == SourceSearchController::class.java) {
                            router.popController(callingController)
                        }
                    }
                    (targetController as? SearchController)?.copyManga(manga, newManga)
                }
                .neutralButton(android.R.string.cancel)
        }
    }

    override fun onTitleClick(source: CatalogueSource) {
        presenter.preferences.lastUsedSource().set(source.id)

        router.pushController(SourceSearchController(manga, source, presenter.query).withFadeTransaction())
    }
}
