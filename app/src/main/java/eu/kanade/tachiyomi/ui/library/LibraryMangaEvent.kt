package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.database.models.Category
import timber.log.Timber

class LibraryMangaEvent(val mangas: Map<Int, List<LibraryItem>>) {

    fun getMangaForCategory(category: Category): List<LibraryItem>? {
        Timber.d("carlos %s  %s", category.name, category.id)
        return mangas[category.id]
    }
}
