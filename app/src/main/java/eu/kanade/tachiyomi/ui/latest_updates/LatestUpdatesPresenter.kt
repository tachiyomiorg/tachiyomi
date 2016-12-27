package eu.kanade.tachiyomi.ui.latest_updates

import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.ui.catalogue.CataloguePresenter
import eu.kanade.tachiyomi.ui.catalogue.Pager
import eu.kanade.tachiyomi.data.source.online.OnlineSource.FilterState

/**
 * Presenter of [LatestUpdatesFragment]. Inherit CataloguePresenter.
 */
class LatestUpdatesPresenter : CataloguePresenter() {

    override fun createPager(query: String, filterStates: List<FilterState>): Pager {
        return LatestUpdatesPager(source)
    }

    override fun getEnabledSources(): List<OnlineSource> {
        return super.getEnabledSources().filter { it.supportsLatest }
    }

    override fun isValidSource(source: Source?): Boolean {
        return super.isValidSource(source) && (source as OnlineSource).supportsLatest
    }

}