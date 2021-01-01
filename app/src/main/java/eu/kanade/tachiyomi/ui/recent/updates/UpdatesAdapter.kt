package eu.kanade.tachiyomi.ui.recent.updates

import android.content.Context
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.chapter.base.BaseChaptersAdapter
import eu.kanade.tachiyomi.util.system.getResourceColor

class UpdatesAdapter(
    val controller: UpdatesController,
    context: Context
) : BaseChaptersAdapter<IFlexible<*>>(controller) {

    var readColor = context.getResourceColor(R.attr.colorOnSurface, 0.38f)
    var unreadColor = context.getResourceColor(R.attr.colorOnSurface)

    val coverClickListener: OnCoverClickListener = controller

    init {
        setDisplayHeadersAtStartUp(true)
        setStickyHeaders(true)
    }

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }
}
