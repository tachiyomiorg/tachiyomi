package eu.kanade.tachiyomi.ui.source

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [SourceController].
 */
class SourceAdapter(val controller: SourceController) :
        FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val cardBackground = controller.activity!!.getResourceColor(R.attr.colorSurface)

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
     * Note: Should only be handled by [SourceController]
     */
    interface OnBrowseClickListener {
        fun onBrowseClick(position: Int)
    }

    /**
     * Listener which should be called when user clicks latest.
     * Note: Should only be handled by [SourceController]
     */
    interface OnLatestClickListener {
        fun onLatestClick(position: Int)
    }
}
