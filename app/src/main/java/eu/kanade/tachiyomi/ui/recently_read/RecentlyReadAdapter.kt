package eu.kanade.tachiyomi.ui.recently_read

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.injectLazy

/**
 * Adapter of RecentlyReadHolder.
 * Connection between Fragment and Holder
 * Holder updates should be called from here.
 *
 * @param controller a RecentlyReadController object
 * @constructor creates an instance of the adapter.
 */
class RecentlyReadAdapter(val controller: RecentlyReadController)
: FlexibleAdapter<RecentlyReadItem>(null, controller, true) {

    val sourceManager by injectLazy<SourceManager>()

    val resumeClickListener: OnResumeClickListener = controller

    val removeClickListener: OnRemoveClickListener = controller

    val coverClickListener: OnCoverClickListener = controller

    interface OnResumeClickListener {
        fun onResumeClick(position: Int)
    }

    interface OnRemoveClickListener {
        fun onRemoveClick(position: Int)
    }

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }
}
