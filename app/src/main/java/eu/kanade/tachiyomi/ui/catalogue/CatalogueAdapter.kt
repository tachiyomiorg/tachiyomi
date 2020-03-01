package eu.kanade.tachiyomi.ui.catalogue

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [CatalogueController].
 */
class CatalogueAdapter(val controller: CatalogueController) :
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
     * Note: Should only be handled by [CatalogueController]
     */
    interface OnBrowseClickListener {
        fun onBrowseClick(position: Int)
    }

    /**
     * Listener which should be called when user clicks latest.
     * Note: Should only be handled by [CatalogueController]
     */
    interface OnLatestClickListener {
        fun onLatestClick(position: Int)
    }
}
