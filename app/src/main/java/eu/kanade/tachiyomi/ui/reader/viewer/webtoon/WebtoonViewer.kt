package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.WebtoonLayoutManager
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import rx.subscriptions.CompositeSubscription
import timber.log.Timber

class WebtoonViewer(activity: ReaderActivity) : BaseViewer(activity) {

    private var scrollDistance = 0

    private val adapter = WebtoonAdapter(this)

    private val layoutManager = WebtoonLayoutManager(activity).apply {
        val screenHeight = activity.resources.displayMetrics.heightPixels
        extraLayoutSpace = screenHeight / 2
        scrollDistance = screenHeight * 3 / 4
    }

    private var currentPage: Any? = null

    private val frame = WebtoonFrame(activity)

    val recycler = WebtoonRecyclerView(activity)

    val config = WebtoonConfig()

    val subscriptions = CompositeSubscription()

    init {
        recycler.visibility = View.GONE // Don't let the recycler layout yet
        recycler.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        recycler.itemAnimator = null
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                val index = layoutManager.findLastEndVisibleItemPosition()
                val item = adapter.items.getOrNull(index)
                if (item != null && currentPage != item) {
                    currentPage = item
                    when (item) {
                        is ReaderPage -> onPageSelected(item)
                        is ChapterTransition -> onTransitionSelected(item)
                    }
                }

                if (dy < 0) {
                    val firstIndex = layoutManager.findFirstVisibleItemPosition()
                    val firstItem = adapter.items.getOrNull(firstIndex)
                    if (firstItem is ChapterTransition.Prev) {
                        activity.requestPreloadPreviousChapter()
                    }
                }
            }
        })
        recycler.tapListener = { event ->
            val positionX = event.rawX
            when {
                positionX < recycler.width * 0.33 -> if (config.tappingEnabled) moveUp()
                positionX > recycler.width * 0.66 -> if (config.tappingEnabled) moveDown()
                else -> activity.toggleMenu()
            }
        }
        recycler.longTapListener = { event ->
            val child = recycler.findChildViewUnder(event.x, event.y)
            val position = recycler.getChildAdapterPosition(child)
            val item = adapter.items.getOrNull(position)
            if (item is ReaderPage) {
                activity.onPageLongTap(item)
            }
        }

        frame.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        frame.addView(recycler)
    }

    override fun getView(): View {
        return frame
    }

    override fun destroy() {
        super.destroy()
        config.unsubscribe()
        subscriptions.unsubscribe()
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
        if (transition is ChapterTransition.Prev) {
            Timber.w("Request preload previous chapter because we're on the transition")
            activity.requestPreloadPreviousChapter()
        }
    }

    override fun setChapters(chapters: ViewerChapters) {
        Timber.w("setChapters")
        adapter.setChapters(chapters)

        if (recycler.visibility == View.GONE) {
            Timber.w("Recycler first layout")
            val pages = chapters.currChapter.pages ?: return
            moveToPage(pages[chapters.currChapter.requestedPage])
            recycler.visibility = View.VISIBLE
        }
    }

    override fun moveToPage(page: ReaderPage) {
        Timber.w("moveToPage")
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            recycler.scrollToPosition(position)
        } else {
            Timber.w("Page $page not found in adapter")
        }
    }

    override fun moveLeft() {
        recycler.smoothScrollBy(0, -scrollDistance)
    }

    override fun moveRight() {
        recycler.smoothScrollBy(0, scrollDistance)
    }

    override fun moveUp() {
        moveLeft()
    }

    override fun moveDown() {
        moveRight()
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

}
