package eu.kanade.tachiyomi.ui.library

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.database.models.Manga

/**
 * Adapter storing a list of manga in a certain category.
 *
 * @param view the fragment containing this adapter.
 */
class LibraryCategoryAdapter(val view: LibraryCategoryView) :
        FlexibleAdapter<LibraryItem>(null, view, true) {

    /**
     * The list of manga in this category.
     */
    private var mangas: List<LibraryItem> = emptyList()

    /**
     * Sets a list of manga in the adapter.
     *
     * @param list the list to set.
     */
    fun setItems(list: List<LibraryItem>) {
        updateDataSet(list.toMutableList())

        // A copy of manga always unfiltered.
        mangas = list.toList()
    }

    /**
     * Returns the position in the adapter for the given manga.
     *
     * @param manga the manga to find.
     */
    fun indexOf(manga: Manga): Int {
        return mangas.indexOfFirst { it.manga.id == manga.id }
    }

    fun performFilter() {
        filterItems(mangas)
    }

}
