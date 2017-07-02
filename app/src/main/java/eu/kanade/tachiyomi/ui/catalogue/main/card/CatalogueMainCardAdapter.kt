package eu.kanade.tachiyomi.ui.catalogue.main.card

import eu.kanade.tachiyomi.ui.catalogue.main.CatalogueMainController

/**
 * Adapter that holds the catalogue items.
 *
 * @param controller instance of [CatalogueMainController].
 */
class CatalogueMainCardAdapter(controller: CatalogueMainController) :
        eu.davidea.flexibleadapter.FlexibleAdapter<CatalogueMainCardItem>(null, controller, true) {

    /**
     * Listen for browse item clicks.
     */
    val browseClickListener: OnBrowseClickListener = controller

    /**
     * List for latest item clicks.
     */
    val latestClickListener: OnLatestClickListener = controller

    /**
     * Listener which should be called when user clicks browse.
     * Note: Should only be handled by [CatalogueMainController]
     */
    interface OnBrowseClickListener {
        fun OnBrowseClickListener(item: CatalogueMainCardItem)
    }

    /**
     * Listener which should be called when user clicks latest.
     * Note: Should only be handled by [CatalogueMainController]
     */
    interface OnLatestClickListener {
        fun OnLatestClickListener(item: CatalogueMainCardItem)
    }
}