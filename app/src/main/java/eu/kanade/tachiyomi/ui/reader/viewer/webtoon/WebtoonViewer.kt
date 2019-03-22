package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.WebtoonLayoutManager
import android.view.KeyEvent
import android.view.MotionEvent
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

/**
 * Implementation of a [BaseViewer] to display pages with a [RecyclerView].
 */
class WebtoonViewer(val activity: ReaderActivity) : BaseViewer {

    /**
     * Recycler view used by this viewer.
     */
    val recycler = WebtoonRecyclerView(activity)

    /**
     * Frame containing the recycler view.
     */
    private val frame = WebtoonFrame(activity)

    /**
     * Layout manager of the recycler view.
     */
    private val layoutManager = WebtoonLayoutManager(activity)

    /**
     * Adapter of the recycler view.
     */
    private val adapter = WebtoonAdapter(this)

    /**
     * Distance to scroll when the user taps on one side of the recycler view.
     */
    private var scrollDistance = activity.resources.displayMetrics.heightPixels * 3 / 4

    /**
     * Currently active item. It can be a chapter page or a chapter transition.
     */
    private var currentPage: Any? = null

    /**
     * Configuration used by this viewer, like allow taps, or crop image borders.
     */
    val config = WebtoonConfig()

    /**
     * Subscriptions to keep while this viewer is used.
     */
    val subscriptions = CompositeSubscription()

    init {
        recycler.visibility = View.GONE // Don't let the recycler layout yet
        recycler.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        recycler.itemAnimator = null
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                val position = layoutManager.findLastEndVisibleItemPosition()
                val item = adapter.items.getOrNull(position)
                if (item != null && currentPage != item) {
                    currentPage = item
                    when (item) {
                        is ReaderPage -> onPageSelected(item, position)
                        is ChapterTransition -> onTransitionSelected(item)
                    }
                }

                if (dy < 0) {
                    val firstIndex = layoutManager.findFirstVisibleItemPosition()
                    val firstItem = adapter.items.getOrNull(firstIndex)
                    if (firstItem is ChapterTransition.Prev && firstItem.to != null) {
                        activity.requestPreloadChapter(firstItem.to)
                    }
                }
            }
        })
        recycler.tapListener = { event ->
            val positionX = event.rawX
            when {
                positionX < recycler.width * 0.33 -> if (config.tappingEnabled) scrollUp()
                positionX > recycler.width * 0.66 -> if (config.tappingEnabled) scrollDown()
                else -> activity.toggleMenu()
            }
        }
        recycler.longTapListener = f@ { event ->
            if (activity.menuVisible || config.longTapEnabled) {
                val child = recycler.findChildViewUnder(event.x, event.y)
                val position = recycler.getChildAdapterPosition(child)
                val item = adapter.items.getOrNull(position)
                if (item is ReaderPage) {
                    activity.onPageLongTap(item)
                    return@f true
                }
            }
            false
        }

        frame.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        frame.addView(recycler)
    }

    /**
     * Returns the view this viewer uses.
     */
    override fun getView(): View {
        return frame
    }

    /**
     * Destroys this viewer. Called when leaving the reader or swapping viewers.
     */
    override fun destroy() {
        super.destroy()
        config.unsubscribe()
        subscriptions.unsubscribe()
    }

    /**
     * Called from the RecyclerView listener when a [page] is marked as active. It notifies the
     * activity of the change and requests the preload of the next chapter if this is the last page.
     */
    private fun onPageSelected(page: ReaderPage, position: Int) {
        val pages = page.chapter.pages!! // Won't be null because it's the loaded chapter
        Timber.d("onPageSelected: ${page.number}/${pages.size}")
        activity.onPageSelected(page)

        if (page === pages.last()) {
            Timber.d("Request preload next chapter because we're at the last page")
            val transition = adapter.items.getOrNull(position + 1) as? ChapterTransition.Next
            if (transition?.to != null) {
                activity.requestPreloadChapter(transition.to)
            }
        }
    }

    /**
     * Called from the RecyclerView listener when a [transition] is marked as active. It request the
     * preload of the destination chapter of the transition.
     */
    private fun onTransitionSelected(transition: ChapterTransition) {
        Timber.d("onTransitionSelected: $transition")
        val toChapter = transition.to
        if (toChapter != null) {
            Timber.d("Request preload destination chapter because we're on the transition")
            activity.requestPreloadChapter(toChapter)
        } else if (transition is ChapterTransition.Next) {
            // No more chapters, show menu because the user is probably going to close the reader
            activity.showMenu()
        }
    }

    /**
     * Tells this viewer to set the given [chapters] as active.
     */
    override fun setChapters(chapters: ViewerChapters) {
        Timber.d("setChapters")
        adapter.setChapters(chapters)

        if (recycler.visibility == View.GONE) {
            Timber.d("Recycler first layout")
            val pages = chapters.currChapter.pages ?: return
            moveToPage(pages[chapters.currChapter.requestedPage])
            recycler.visibility = View.VISIBLE
        }
    }

    /**
     * Tells this viewer to move to the given [page].
     */
    override fun moveToPage(page: ReaderPage) {
        Timber.d("moveToPage")
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            recycler.scrollToPosition(position)
        } else {
            Timber.d("Page $page not found in adapter")
        }
    }

    /**
     * Scrolls up by [scrollDistance].
     */
    private fun scrollUp() {
        recycler.smoothScrollBy(0, -scrollDistance)
    }

    /**
     * Scrolls down by [scrollDistance].
     */
    private fun scrollDown() {
        recycler.smoothScrollBy(0, scrollDistance)
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) scrollDown() else scrollUp()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) scrollUp() else scrollDown()
                }
            }
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) scrollUp()

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) scrollDown()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        return false
    }

}
