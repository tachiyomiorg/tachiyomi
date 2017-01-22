package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.support.v4.content.ContextCompat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.base.BaseReader
import eu.kanade.tachiyomi.ui.reader.viewer.pager.horizontal.LeftToRightReader
import eu.kanade.tachiyomi.ui.reader.viewer.pager.horizontal.RightToLeftReader
import rx.subscriptions.CompositeSubscription

/**
 * Implementation of a reader based on a ViewPager.
 */
abstract class PagerReader : BaseReader() {

    companion object {
        /**
         * Zoom automatic alignment.
         */
        const val ALIGN_AUTO = 1

        /**
         * Align to left.
         */
        const val ALIGN_LEFT = 2

        /**
         * Align to right.
         */
        const val ALIGN_RIGHT = 3

        /**
         * Align to right.
         */
        const val ALIGN_CENTER = 4

        /**
         * Left side region of the screen. Used for touch events.
         */
        const val LEFT_REGION = 0.33f

        /**
         * Right side region of the screen. Used for touch events.
         */
        const val RIGHT_REGION = 0.66f
    }

    /**
     * Generic interface of a ViewPager.
     */
    lateinit var pager: Pager
        private set

    /**
     * Adapter of the pager.
     */
    lateinit var adapter: PagerReaderAdapter
        private set

    /**
     * Gesture detector for touch events.
     */
    val gestureDetector by lazy { GestureDetector(context, ImageGestureListener()) }

    /**
     * Subscriptions for reader settings.
     */
    var subscriptions: CompositeSubscription? = null
        private set

    /**
     * Whether transitions are enabled or not.
     */
    var transitions: Boolean = false
        private set

    /**
     * Scale type (fit width, fit screen, etc).
     */
    var scaleType = 1
        private set

    /**
     * Zoom type (start position).
     */
    var zoomType = 1
        private set

    /**
     * Text color for black theme.
     */
    val whiteColor by lazy { ContextCompat.getColor(context, R.color.textColorSecondaryDark) }

    /**
     * Text color for white theme.
     */
    val blackColor by lazy { ContextCompat.getColor(context, R.color.textColorSecondaryLight) }

    /**
     * Initializes the pager.
     *
     * @param pager the pager to initialize.
     */
    protected fun initializePager(pager: Pager) {
        adapter = PagerReaderAdapter(this)

        this.pager = pager.apply {
            setLayoutParams(ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            setOffscreenPageLimit(1)
            setId(R.id.view_pager)
            setOnChapterBoundariesOutListener(object : OnChapterBoundariesOutListener {
                override fun onFirstPageOutEvent() {
                    readerActivity.requestPreviousChapter()
                }

                override fun onLastPageOutEvent() {
                    readerActivity.requestNextChapter()
                }
            })
            setOnPageChangeListener { onPageChanged(it) }
        }
        pager.adapter = adapter

        subscriptions = CompositeSubscription().apply {
            val preferences = readerActivity.preferences

            add(preferences.imageDecoder()
                    .asObservable()
                    .doOnNext { setDecoderClass(it) }
                    .skip(1)
                    .distinctUntilChanged()
                    .subscribe { refreshAdapter() })

            add(preferences.zoomStart()
                    .asObservable()
                    .doOnNext { setZoomStart(it) }
                    .skip(1)
                    .distinctUntilChanged()
                    .subscribe { refreshAdapter() })

            add(preferences.imageScaleType()
                    .asObservable()
                    .doOnNext { scaleType = it }
                    .skip(1)
                    .distinctUntilChanged()
                    .subscribe { refreshAdapter() })

            add(preferences.enableTransitions()
                    .asObservable()
                    .subscribe { transitions = it })
        }

        setPagesOnAdapter()
    }

    override fun onDestroyView() {
        pager.clearOnPageChangeListeners()
        subscriptions?.unsubscribe()
        super.onDestroyView()
    }

    /**
     * Gesture detector for Subsampling Scale Image View.
     */
    inner class ImageGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (isAdded) {
                val positionX = e.x

                if (positionX < pager.width * LEFT_REGION) {
                    if (tappingEnabled) moveLeft()
                } else if (positionX > pager.width * RIGHT_REGION) {
                    if (tappingEnabled) moveRight()
                } else {
                    readerActivity.toggleMenu()
                }
            }
            return true
        }
    }

    /**
     * Called when a new chapter is set in [BaseReader].
     *
     * @param chapter the chapter set.
     * @param currentPage the initial page to display.
     */
    override fun onChapterSet(chapter: ReaderChapter, currentPage: Page) {
        this.currentPage = getPageIndex(currentPage) // we might have a new page object

        // Make sure the view is already initialized.
        if (view != null) {
            setPagesOnAdapter()
        }
    }

    /**
     * Called when a chapter is appended in [BaseReader].
     *
     * @param chapter the chapter appended.
     */
    override fun onChapterAppended(chapter: ReaderChapter) {
        // Make sure the view is already initialized.
        if (view != null) {
            adapter.pages = pages
        }
    }

    /**
     * Sets the pages on the adapter.
     */
    protected fun setPagesOnAdapter() {
        if (pages.isNotEmpty()) {
            adapter.pages = pages
            if (currentPage == pager.currentItem) {
                onPageChanged(currentPage)
            } else {
                setActivePage(currentPage)
            }
        }
    }

    /**
     * Sets the active page.
     *
     * @param pageNumber the index of the page from [pages].
     */
    override fun setActivePage(pageNumber: Int) {
        pager.setCurrentItem(pageNumber, false)
    }

    /**
     * Refresh the adapter.
     */
    private fun refreshAdapter() {
        pager.adapter = adapter
        pager.setCurrentItem(currentPage, false)
    }

    /**
     * Moves a page to the right.
     */
    override fun moveRight() {
        moveToNext()
    }

    /**
     * Moves a page to the left.
     */
    override fun moveLeft() {
        moveToPrevious()
    }

    /**
     * Moves to the next page or requests the next chapter if it's the last one.
     */
    protected fun moveToNext() {
        if (pager.currentItem != pager.adapter.count - 1) {
            pager.setCurrentItem(pager.currentItem + 1, transitions)
        } else {
            readerActivity.requestNextChapter()
        }
    }

    /**
     * Moves to the previous page or requests the previous chapter if it's the first one.
     */
    protected fun moveToPrevious() {
        if (pager.currentItem != 0) {
            pager.setCurrentItem(pager.currentItem - 1, transitions)
        } else {
            readerActivity.requestPreviousChapter()
        }
    }

    /**
     * Sets the zoom start position.
     *
     * @param zoomStart the value stored in preferences.
     */
    private fun setZoomStart(zoomStart: Int) {
        if (zoomStart == ALIGN_AUTO) {
            if (this is LeftToRightReader)
                setZoomStart(ALIGN_LEFT)
            else if (this is RightToLeftReader)
                setZoomStart(ALIGN_RIGHT)
            else
                setZoomStart(ALIGN_CENTER)
        } else {
            zoomType = zoomStart
        }
    }

}
