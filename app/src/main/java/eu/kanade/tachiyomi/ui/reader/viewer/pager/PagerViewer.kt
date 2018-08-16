package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.support.v4.view.ViewPager
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import timber.log.Timber

@Suppress("LeakingThis")
abstract class PagerViewer(activity: ReaderActivity) : BaseViewer(activity) {

    val pager = createPager()

    val config = PagerConfig(this)

    private val adapter = PagerViewerAdapter(this)

    private var awaitingIdleViewerChapters: ViewerChapters? = null

    private var currentPage: Any? = null

    private var isIdle = true
        set(value) {
            field = value
            if (value) {
                awaitingIdleViewerChapters?.let {
                    setChaptersInternal(it)
                    awaitingIdleViewerChapters = null
                }
            }
        }

    init {
        pager.visibility = View.GONE // Don't layout the pager yet
        pager.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        pager.offscreenPageLimit = 1
        pager.id = R.id.reader_pager
        pager.adapter = adapter
        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                val page = adapter.items.getOrNull(position)
                if (page != null && currentPage != page) {
                    currentPage = page
                    when (page) {
                        is ReaderPage -> onPageSelected(page)
                        is ChapterTransition -> onTransitionSelected(page)
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                isIdle = state == ViewPager.SCROLL_STATE_IDLE
            }
        })
        pager.tapListener = { event ->
            val positionX = event.x
            when {
                positionX < pager.width * 0.33f -> if (config.tappingEnabled) moveLeft()
                positionX > pager.width * 0.66f -> if (config.tappingEnabled) moveRight()
                else -> activity.toggleMenu()
            }
        }
        pager.longTapListener = {
            val item = adapter.items.getOrNull(pager.currentItem)
            if (item is ReaderPage) {
                activity.onPageLongTap(item)
            }
        }

        config.imagePropertyChangedListener = {
            refreshAdapter()
        }
    }

    abstract fun createPager(): Pager

    override fun getView(): View {
        return pager
    }

    override fun destroy() {
        super.destroy()
        config.unsubscribe()
    }

    private fun onPageSelected(page: ReaderPage) {
        val pages = page.chapter.pages!! // Won't be null because it's the loaded chapter
        Timber.w("onPageSelected: ${page.number}/${pages.size}")
        activity.onPageSelected(page)

        if (page === pages.last()) {
            Timber.w("Request preload next chapter because we're at the last page")
            activity.requestPreloadNextChapter()
        }
    }

    private fun onTransitionSelected(transition: ChapterTransition) {
        Timber.w("onTransitionSelected: $transition")
        when (transition) {
            is ChapterTransition.Prev -> {
                Timber.w("Request preload previous chapter because we're on the transition")
                activity.requestPreloadPreviousChapter()
            }
            is ChapterTransition.Next -> {
                Timber.w("Request preload next chapter because we're on the transition")
                activity.requestPreloadNextChapter()
            }
        }
    }

    override fun setChapters(chapters: ViewerChapters) {
        if (isIdle) {
            setChaptersInternal(chapters)
        } else {
            awaitingIdleViewerChapters = chapters
        }
    }

    private fun setChaptersInternal(chapters: ViewerChapters) {
        Timber.w("setChaptersInternal")
        adapter.setChapters(chapters)

        // Layout the pager once a chapter is being set
        if (pager.visibility == View.GONE) {
            Timber.w("Pager first layout")
            val pages = chapters.currChapter.pages ?: return
            moveToPage(pages[chapters.currChapter.requestedPage])
            pager.visibility = View.VISIBLE
        }
    }

    override fun moveToPage(page: ReaderPage) {
        Timber.w("moveToPage")
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            pager.setCurrentItem(position, true)
        } else {
            Timber.w("Page $page not found in adapter")
        }
    }

    open fun moveToNext() {
        moveRight()
    }

    open fun moveToPrevious() {
        moveLeft()
    }

    override fun moveRight() {
        if (pager.currentItem != adapter.count - 1) {
            pager.setCurrentItem(pager.currentItem + 1, config.usePageTransitions)
        }
    }

    override fun moveLeft() {
        if (pager.currentItem != 0) {
            pager.setCurrentItem(pager.currentItem - 1, config.usePageTransitions)
        }
    }

    override fun moveUp() {
        moveToPrevious()
    }

    override fun moveDown() {
        moveToNext()
    }

    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (activity.menuVisible) {
                    return false
                } else if (config.volumeKeysEnabled && isUp) {
                    if (!config.volumeKeysInverted) moveDown() else moveUp()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (activity.menuVisible) {
                    return false
                } else if (config.volumeKeysEnabled && isUp) {
                    if (!config.volumeKeysInverted) moveUp() else moveDown()
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (isUp) moveRight()
            KeyEvent.KEYCODE_DPAD_LEFT -> if (isUp) moveLeft()
            KeyEvent.KEYCODE_DPAD_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_DPAD_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()
            else -> return false
        }
        return true
    }

    private fun refreshAdapter() {
        val currentItem = pager.currentItem
        pager.adapter = adapter
        pager.setCurrentItem(currentItem, false)
    }

}
