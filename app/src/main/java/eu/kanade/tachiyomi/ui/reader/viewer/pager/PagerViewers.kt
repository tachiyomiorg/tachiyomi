package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.ReaderActivity

class L2RPagerViewer(activity: ReaderActivity) : PagerViewer(activity) {
    override fun createPager(): Pager {
        return Pager(activity)
    }
}

class R2LPagerViewer(activity: ReaderActivity) : PagerViewer(activity) {
    override fun createPager(): Pager {
        return Pager(activity)
    }

    override fun moveToNext() {
        moveLeft()
    }

    override fun moveToPrevious() {
        moveRight()
    }
}

class VerticalPagerViewer(activity: ReaderActivity) : PagerViewer(activity) {
    override fun createPager(): Pager {
        return Pager(activity, isHorizontal = false)
    }
}
