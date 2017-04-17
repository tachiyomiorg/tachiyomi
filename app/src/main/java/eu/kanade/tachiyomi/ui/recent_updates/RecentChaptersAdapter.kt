package eu.kanade.tachiyomi.ui.recent_updates

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

class RecentChaptersAdapter(val controller: RecentChaptersController) :
        FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    init {
        setDisplayHeadersAtStartUp(true)
        setStickyHeaders(true)
    }
}