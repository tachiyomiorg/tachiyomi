package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import timber.log.Timber

class PagerViewerAdapter(private val viewer: PagerViewer) : ViewPagerAdapter() {

    var items: List<Any> = emptyList()
        private set

    fun setChapters(chapters: ViewerChapters) {
        val newItems = mutableListOf<Any>()

        if (chapters.prevChapter != null) {
            // We only need to add the last few pages of the previous chapter, because it'll be
            // selected as the current chapter when one of those pages is selected.
            val prevPages = chapters.prevChapter.pages
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(2))
            }
        }
        newItems.add(
                ChapterTransition.Prev(chapters.currChapter,
                        chapters.prevChapter))

        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            newItems.addAll(currPages)
        }

        newItems.add(
                ChapterTransition.Next(chapters.currChapter,
                        chapters.nextChapter))
        if (chapters.nextChapter != null) {
            // Add at most two pages, because this chapter will be selected before the user can
            // swap more pages.
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        if (viewer is R2LPagerViewer) {
            newItems.reverse()
        }

        items = newItems
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun createView(container: ViewGroup, position: Int): View {
        val item = items[position]
        return when (item) {
            is ReaderPage -> PagerPageHolder(viewer, item)
            is ChapterTransition -> PagerTransitionHolder(viewer, item)
            else -> error("Oops")
        }
    }

    override fun getItemPosition(view: Any): Int {
        if (view is PositionableView) {
            val position = items.indexOf(view.item)
            if (position != -1) {
                return position
            } else {
                Timber.w("Position for ${view.item} not found")
            }
        }
        Timber.w("Position none!")
        return PagerAdapter.POSITION_NONE
    }



}
