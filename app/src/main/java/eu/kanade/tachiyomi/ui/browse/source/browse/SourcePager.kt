package eu.kanade.tachiyomi.ui.browse.source.browse

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.util.lang.awaitSingle

open class SourcePager(val source: CatalogueSource, val query: String, val filters: FilterList) : Pager() {

    override suspend fun requestNextPage() {
        val page = currentPage

        val observable = if (query.isBlank() && filters.isEmpty()) {
            source.fetchPopularManga(page)
        } else {
            source.fetchSearchManga(page, query, filters)
        }

        val mangasPage = observable.awaitSingle()

        if (mangasPage.mangas.isNotEmpty()) {
            onPageReceived(mangasPage)
        } else {
            throw NoResultsException()
        }
    }
}
