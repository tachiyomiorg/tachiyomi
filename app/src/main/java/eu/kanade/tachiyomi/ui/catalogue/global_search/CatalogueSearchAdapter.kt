package eu.kanade.tachiyomi.ui.catalogue.global_search

import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Adapter that holds the search cards.
 *
 * @param controller instance of [CatalogueSearchController].
 */
class CatalogueSearchAdapter(val controller: CatalogueSearchController) :
        FlexibleAdapter<CatalogueSearchItem>(null, controller, true)