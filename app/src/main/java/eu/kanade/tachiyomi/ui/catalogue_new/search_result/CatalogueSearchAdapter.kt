package eu.kanade.tachiyomi.ui.catalogue_new.search_result

import eu.davidea.flexibleadapter.FlexibleAdapter

class CatalogueSearchAdapter(val controller: CatalogueSearchController) :
        FlexibleAdapter<CatalogueSearchItem>(null, controller, true)