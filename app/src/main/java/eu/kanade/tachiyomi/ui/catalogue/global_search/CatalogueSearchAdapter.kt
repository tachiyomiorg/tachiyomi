package eu.kanade.tachiyomi.ui.catalogue.global_search

import eu.davidea.flexibleadapter.FlexibleAdapter

class CatalogueSearchAdapter(val controller: CatalogueSearchController) :
        FlexibleAdapter<CatalogueSearchItem>(null, controller, true)