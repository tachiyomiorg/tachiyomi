package eu.kanade.tachiyomi.ui.catalogue.main

import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [CatalogueMainController].
 */
class CatalogueMainAdapter(val controller: CatalogueMainController) :
        FlexibleAdapter<CatalogueMainItem>(null, controller, true)

