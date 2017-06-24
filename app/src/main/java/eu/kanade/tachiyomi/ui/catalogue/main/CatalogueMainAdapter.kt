package eu.kanade.tachiyomi.ui.catalogue.main

import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Adapter containing sources for [CatalogueMainItem]
 */
class CatalogueMainAdapter(val controller: CatalogueMainController) :
        FlexibleAdapter<CatalogueMainItem>(null, controller, true)

