package eu.kanade.tachiyomi.ui.catalogue.main

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.getResourceColor

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [CatalogueMainController].
 */
class CatalogueMainAdapter(val controller: CatalogueMainController) :
        FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val cardBackground = controller.activity!!.getResourceColor(R.attr.background_card)

    init {
        setDisplayHeadersAtStartUp(true)
    }

    /**
     * Listener for browse item clicks.
     */
    val browseClickListener: OnBrowseClickListener = controller

    /**
     * Listener for latest item clicks.
     */
    val latestClickListener: OnLatestClickListener = controller

    /**
     * Listener which should be called when user clicks browse.
     * Note: Should only be handled by [CatalogueMainController]
     */
    interface OnBrowseClickListener {
        fun onBrowseClick(position: Int)
    }

    /**
     * Listener which should be called when user clicks latest.
     * Note: Should only be handled by [CatalogueMainController]
     */
    interface OnLatestClickListener {
        fun onLatestClick(position: Int)
    }
}

