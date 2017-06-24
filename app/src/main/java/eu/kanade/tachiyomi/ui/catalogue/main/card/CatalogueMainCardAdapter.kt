package eu.kanade.tachiyomi.ui.catalogue.main.card

import eu.kanade.tachiyomi.ui.catalogue.main.CatalogueMainController

class CatalogueMainCardAdapter(controller: CatalogueMainController) :
        eu.davidea.flexibleadapter.FlexibleAdapter<CatalogueMainCardItem>(null, controller, true) {

    val browseClickListener: OnBrowseClickListener = controller

    val latestClickListener: OnLatestClickListener = controller

    interface OnBrowseClickListener {
        fun OnBrowseClickListener(item: CatalogueMainCardItem)
    }

    interface OnLatestClickListener {
        fun OnLatestClickListener(item: CatalogueMainCardItem)
    }
}