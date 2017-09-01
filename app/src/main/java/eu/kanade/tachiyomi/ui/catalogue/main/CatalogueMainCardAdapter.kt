package eu.kanade.tachiyomi.ui.catalogue.main

import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Adapter that holds the catalogue items.
 *
 * @param controller instance of [CatalogueMainController].
 */
class CatalogueMainCardAdapter(controller: CatalogueMainController) :
        FlexibleAdapter<CatalogueMainCardItem>(null, controller, true) {

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
        fun onBrowseClickListener(item: CatalogueMainCardItem)
    }

    /**
     * Listener which should be called when user clicks latest.
     * Note: Should only be handled by [CatalogueMainController]
     */
    interface OnLatestClickListener {
        fun OnLatestClickListener(item: CatalogueMainCardItem)
    }
}