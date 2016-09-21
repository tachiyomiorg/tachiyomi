package eu.kanade.tachiyomi.ui.latest_updates

import eu.kanade.tachiyomi.ui.catalogue.CataloguePresenter
import eu.kanade.tachiyomi.ui.catalogue.Pager
import eu.kanade.tachiyomi.data.source.online.OnlineSource.Filter

/**
 * Presenter of [*Fragment]. Inherit CataloguePresenter.
 */
class LatestUpdatesPresenter : CataloguePresenter() {

    override fun createPager(query: String, filters: List<Filter>): Pager {
        return LatestUpdatesPager(source)
    }

}