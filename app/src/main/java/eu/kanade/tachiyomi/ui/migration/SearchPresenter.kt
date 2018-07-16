package eu.kanade.tachiyomi.ui.migration

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchPresenter

class SearchPresenter(
        initialQuery: String? = "",
        private val manga: Manga
) : CatalogueSearchPresenter(initialQuery) {

    override fun getEnabledSources(): List<CatalogueSource> {
        // Put the source of the selected manga at the top
        return super.getEnabledSources()
                .sortedByDescending { it.id == manga.source }
    }
}