package eu.kanade.tachiyomi.ui.recent.updates

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

class UpdatesAdapter(val controller: UpdatesController) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val coverClickListener: OnCoverClickListener = controller

    init {
        setDisplayHeadersAtStartUp(true)
        setStickyHeaders(true)
    }

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }
}
