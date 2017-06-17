package eu.kanade.tachiyomi.ui.catalogue.main

import eu.davidea.flexibleadapter.FlexibleAdapter

class CatalogueMainAdapter(val controller: CatalogueMainController) :
        FlexibleAdapter<CatalogueMainItem>(null, controller, true)