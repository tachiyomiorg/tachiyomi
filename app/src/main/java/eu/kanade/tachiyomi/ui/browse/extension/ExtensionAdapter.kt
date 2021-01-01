package eu.kanade.tachiyomi.ui.browse.extension

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [ExtensionController].
 */
class ExtensionAdapter(controller: ExtensionController) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    init {
        setDisplayHeadersAtStartUp(true)
    }

    /**
     * Listener for browse item clicks.
     */
    val buttonClickListener: OnButtonClickListener = controller

    interface OnButtonClickListener {
        fun onButtonClick(position: Int)
    }
}
