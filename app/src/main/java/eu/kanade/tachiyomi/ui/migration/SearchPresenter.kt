package eu.kanade.tachiyomi.ui.migration

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchPresenter

class SearchPresenter(
        initialQuery: String? = "",
        private val manga: Manga
) : CatalogueSearchPresenter(initialQuery) {

    override fun getEnabledSources(): List<CatalogueSource> {
        // Filter out the source of the selected manga
        return super.getEnabledSources()
                .filterNot { it.id == manga.source }
    }
}