package eu.kanade.tachiyomi.ui.migration

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchCardItem
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchItem
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

    override fun createCatalogueSearchItem(source: CatalogueSource, results: List<CatalogueSearchCardItem>?): CatalogueSearchItem {
        //Set the catalogue search item as highlighted if the source matches that of the selected manga
        return CatalogueSearchItem(source, results, source.id == manga.source)
    }

    override fun networkToLocalManga(sManga: SManga, sourceId: Long): Manga {
        val localManga = super.networkToLocalManga(sManga, sourceId)
        // For migration, displayed title should always match source rather than local DB
        localManga.title = sManga.title
        return localManga
    }
}
