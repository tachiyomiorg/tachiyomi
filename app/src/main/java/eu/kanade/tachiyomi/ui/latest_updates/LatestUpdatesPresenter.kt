package eu.kanade.tachiyomi.ui.latest_updates

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.catalogue.CataloguePresenter
import eu.kanade.tachiyomi.ui.catalogue.Pager

/**
 * Presenter of [LatestUpdatesController]. Inherit CataloguePresenter.
 */
class LatestUpdatesPresenter(sourceId: Long) : CataloguePresenter(sourceId) {

    override fun createPager(query: String, filters: FilterList): Pager {
        return LatestUpdatesPager(source)
    }

}