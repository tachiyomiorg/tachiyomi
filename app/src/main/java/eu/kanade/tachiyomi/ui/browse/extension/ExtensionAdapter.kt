package eu.kanade.tachiyomi.ui.browse.extension

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [ExtensionController].
 */
class ExtensionAdapter(val controller: ExtensionController) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val cardBackground = controller.activity!!.getResourceColor(R.attr.colorSurface)

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
