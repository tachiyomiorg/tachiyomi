package eu.kanade.tachiyomi.ui.browse.source

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [SourceController].
 */
class SourceAdapter(controller: SourceController) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    init {
        setDisplayHeadersAtStartUp(true)
    }

    /**
     * Listener for browse item clicks.
     */
    val clickListener: OnSourceClickListener = controller

    /**
     * Listener which should be called when user clicks browse.
     * Note: Should only be handled by [SourceController]
     */
    interface OnSourceClickListener {
        fun onBrowseClick(position: Int)
        fun onLatestClick(position: Int)
        fun onPinClick(position: Int)
    }
}
