package eu.kanade.tachiyomi.ui.latest_updates

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.catalogue.CataloguePresenter
import eu.kanade.tachiyomi.ui.catalogue.Pager

/**
 * Presenter of [LatestUpdatesFragment]. Inherit CataloguePresenter.
 */
class LatestUpdatesPresenter : CataloguePresenter() {

    override fun createPager(query: String, filters: FilterList): Pager {
        return LatestUpdatesPager(source)
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        return super.getEnabledSources().filter { it.supportsLatest }
    }

    override fun isValidSource(source: Source?): Boolean {
        return super.isValidSource(source) && (source as CatalogueSource).supportsLatest
    }

}