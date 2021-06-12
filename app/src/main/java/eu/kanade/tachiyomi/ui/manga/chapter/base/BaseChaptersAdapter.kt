package eu.kanade.tachiyomi.ui.manga.chapter.base

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

abstract class BaseChaptersAdapter<T : IFlexible<*>>(
    controller: OnChapterClickListener
) : FlexibleAdapter<T>(null, controller, true) {

    /**
     * Listener for browse item clicks.
     */
    val clickListener: OnChapterClickListener = controller

    /**
     * Listener which should be called when user clicks the download icons.
     */
    interface OnChapterClickListener {
        fun downloadChapter(position: Int)
        fun deleteChapter(position: Int)
        fun startDownloadNow(position: Int)
    }
}
