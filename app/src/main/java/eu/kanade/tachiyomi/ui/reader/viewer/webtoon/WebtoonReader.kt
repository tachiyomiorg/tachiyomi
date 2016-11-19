package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.base.BaseReader
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.PreCachingLayoutManager
import rx.subscriptions.CompositeSubscription

/**
 * Implementation of a reader for webtoons based on a RecyclerView.
 */
class WebtoonReader : BaseReader() {

    companion object {
        /**
         * Key to save and restore the position of the layout manager.
         */
        private val SAVED_POSITION = "saved_position"

        /**
         * Left side region of the screen. Used for touch events.
         */
        private val LEFT_REGION = 0.33f

        /**
         * Right side region of the screen. Used for touch events.
         */
        private val RIGHT_REGION = 0.66f
    }

    /**
     * RecyclerView of the reader.
     */
    lateinit var recycler: RecyclerView
        private set

    /**
     * Adapter of the recycler.
     */
    lateinit var adapter: WebtoonAdapter
        private set

    /**
     * Layout manager of the recycler.
     */
    lateinit var layoutManager: PreCachingLayoutManager
        private set

    /**
     * Gesture detector for touch events.
     */
    val gestureDetector by lazy { createGestureDetector() }

    /**
     * Subscriptions used while the view exists.
     */
    lateinit var subscriptions: CompositeSubscription
        private set

    private var scrollDistance: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        adapter = WebtoonAdapter(this)

        val screenHeight = resources.displayMetrics.heightPixels
        scrollDistance = screenHeight * 3 / 4

        layoutManager = PreCachingLayoutManager(activity)
        layoutManager.extraLayoutSpace = screenHeight / 2
        if (savedState != null) {
            layoutManager.scrollToPositionWithOffset(savedState.getInt(SAVED_POSITION), 0)
        }

        recycler = RecyclerView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            itemAnimator = null
        }
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                val index = layoutManager.findLastVisibleItemPosition()
                if (index != currentPage) {
                    pages.getOrNull(index)?.let { onPageChanged(index) }
                }
            }
        })

        subscriptions = CompositeSubscription()
        subscriptions.add(readerActivity.preferences.imageDecoder()
                .asObservable()
                .doOnNext { setDecoderClass(it) }
                .skip(1)
                .distinctUntilChanged()
                .subscribe {
                    val activePage = layoutManager.findFirstVisibleItemPosition()
                    recycler.adapter = adapter
                    setActivePage(activePage)
                })

        setPagesOnAdapter()
        return recycler
    }

    override fun onDestroyView() {
        subscriptions.unsubscribe()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val savedPosition = pages.getOrNull(layoutManager.findFirstVisibleItemPosition())?.index ?: 0
        outState.putInt(SAVED_POSITION, savedPosition)
        super.onSaveInstanceState(outState)
    }

    /**
     * Creates the gesture detector for the reader.
     *
     * @return a gesture detector.
     */
    protected fun createGestureDetector(): GestureDetector {
        return GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isAdded) {
                    val positionX = e.x

                    if (positionX < recycler.width * LEFT_REGION) {
                        if (tappingEnabled) moveToPrevious()
                    } else if (positionX > recycler.width * RIGHT_REGION) {
                        if (tappingEnabled) moveToNext()
                    } else {
                        readerActivity.toggleMenu()
                    }
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (isAdded) {
                    val child = recycler.findChildViewUnder(e.rawX, e.rawY)
                    val position = recycler.getChildAdapterPosition(child)
                    val page = adapter.pages?.getOrNull(position)
                    if (page != null)
                        readerActivity.onLongPress(page)
                    else
                        context.toast(getString(R.string.unknown_error))
                }
            }
        })
    }

    /**
     * Called when a new chapter is set in [BaseReader].
     * @param chapter the chapter set.
     * @param currentPage the initial page to display.
     */
    override fun onChapterSet(chapter: ReaderChapter, currentPage: Page) {
        this.currentPage = currentPage.index

        // Make sure the view is already initialized.
        if (view != null) {
            setPagesOnAdapter()
        }
    }

    /**
     * Called when a chapter is appended in [BaseReader].
     * @param chapter the chapter appended.
     */
    override fun onChapterAppended(chapter: ReaderChapter) {
        // Make sure the view is already initialized.
        if (view != null) {
            val insertStart = pages.size - chapter.pages!!.size
            adapter.notifyItemRangeInserted(insertStart, chapter.pages!!.size)
        }
    }

    /**
     * Sets the pages on the adapter.
     */
    private fun setPagesOnAdapter() {
        if (pages.isNotEmpty()) {
            adapter.pages = pages
            recycler.adapter = adapter
            onPageChanged(currentPage)
        }
    }

    /**
     * Sets the active page.
     * @param pageNumber the index of the page from [pages].
     */
    override fun setActivePage(pageNumber: Int) {
        recycler.scrollToPosition(pageNumber)
    }

    /**
     * Moves to the next page or requests the next chapter if it's the last one.
     */
    override fun moveToNext() {
        recycler.smoothScrollBy(0, scrollDistance)
    }

    /**
     * Moves to the previous page or requests the previous chapter if it's the first one.
     */
    override fun moveToPrevious() {
        recycler.smoothScrollBy(0, -scrollDistance)
    }

}
