package eu.kanade.tachiyomi.ui.reader.viewer.base

import com.davemorrissey.labs.subscaleview.decoder.*
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.base.fragment.BaseFragment
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderChapter
import java.util.*

/**
 * Base reader containing the common data that can be used by its implementations. It does not
 * contain any UI related action.
 */
abstract class BaseReader : BaseFragment() {

    companion object {
        /**
         * Image decoder.
         */
        const val IMAGE_DECODER = 0

        /**
         * Rapid decoder.
         */
        const val RAPID_DECODER = 1

        /**
         * Skia decoder.
         */
        const val SKIA_DECODER = 2
    }

    /**
     * List of chapters added in the reader.
     */
    private val chapters = ArrayList<ReaderChapter>()

    /**
     * List of pages added in the reader. It can contain pages from more than one chapter.
     */
    var pages: MutableList<Page> = ArrayList()
        private set

    /**
     * Current visible position of [pages].
     */
    var currentPage: Int = 0
        protected set

    /**
     * Region decoder class to use.
     */
    lateinit var regionDecoderClass: Class<out ImageRegionDecoder>
        private set

    /**
     * Bitmap decoder class to use.
     */
    lateinit var bitmapDecoderClass: Class<out ImageDecoder>
        private set

    /**
     * Whether tap navigation is enabled or not.
     */
    val tappingEnabled by lazy { readerActivity.preferences.readWithTapping().getOrDefault() }

    /**
     * Whether the reader has requested to append a chapter. Used with seamless mode to avoid
     * restarting requests when changing pages.
     */
    private var hasRequestedNextChapter: Boolean = false

    /**
     * Returns the active page.
     */
    fun getActivePage(): Page? {
        return pages.getOrNull(currentPage)
    }

    /**
     * Called when a page changes. Implementations must call this method.
     *
     * @param position the new current page.
     */
    fun onPageChanged(position: Int) {
        val oldPage = pages[currentPage]
        val newPage = pages[position]

        val oldChapter = oldPage.chapter
        val newChapter = newPage.chapter

        // Update page indicator and seekbar
        readerActivity.onPageChanged(newPage)

        // Active chapter has changed.
        if (oldChapter.id != newChapter.id) {
            readerActivity.onEnterChapter(newPage.chapter, newPage.index)
        }
        // Request next chapter only when the conditions are met.
        if (pages.size - position < 5 && chapters.last().id == newChapter.id
                && readerActivity.presenter.hasNextChapter() && !hasRequestedNextChapter) {
            hasRequestedNextChapter = true
            readerActivity.presenter.appendNextChapter()
        }

        currentPage = position
    }

    /**
     * Sets the active page.
     *
     * @param page the page to display.
     */
    fun setActivePage(page: Page) {
        setActivePage(getPageIndex(page))
    }

    /**
     * Searchs for the index of a page in the current list without requiring them to be the same
     * object.
     *
     * @param search the page to search.
     * @return the index of the page in [pages] or 0 if it's not found.
     */
    fun getPageIndex(search: Page): Int {
        for ((index, page) in pages.withIndex()) {
            if (page.index == search.index && page.chapter.id == search.chapter.id) {
                return index
            }
        }
        return 0
    }

    /**
     * Called from the presenter when the page list of a chapter is ready. This method is called
     * on every [onResume], so we add some logic to avoid duplicating chapters.
     *
     * @param chapter the chapter to set.
     * @param currentPage the initial page to display.
     */
    fun onPageListReady(chapter: ReaderChapter, currentPage: Page) {
        if (!chapters.contains(chapter)) {
            // if we reset the loaded page we also need to reset the loaded chapters
            chapters.clear()
            chapters.add(chapter)
            pages = ArrayList(chapter.pages)
            onChapterSet(chapter, currentPage)
        } else {
            setActivePage(currentPage)
        }
    }

    /**
     * Called from the presenter when the page list of a chapter to append is ready. This method is
     * called on every [onResume], so we add some logic to avoid duplicating chapters.
     *
     * @param chapter the chapter to append.
     */
    fun onPageListAppendReady(chapter: ReaderChapter) {
        if (!chapters.contains(chapter)) {
            hasRequestedNextChapter = false
            chapters.add(chapter)
            pages.addAll(chapter.pages!!)
            onChapterAppended(chapter)
        }
    }

    /**
     * Sets the active page.
     *
     * @param pageNumber the index of the page from [pages].
     */
    abstract fun setActivePage(pageNumber: Int)

    /**
     * Called when a new chapter is set in [BaseReader].
     *
     * @param chapter the chapter set.
     * @param currentPage the initial page to display.
     */
    abstract fun onChapterSet(chapter: ReaderChapter, currentPage: Page)

    /**
     * Called when a chapter is appended in [BaseReader].
     *
     * @param chapter the chapter appended.
     */
    abstract fun onChapterAppended(chapter: ReaderChapter)

    /**
     * Moves pages to right. Implementations decide how to move (by a page, by some distance...).
     */
    abstract fun moveRight()

    /**
     * Moves pages to left. Implementations decide how to move (by a page, by some distance...).
     */
    abstract fun moveLeft()

    /**
     * Moves pages down. Implementations decide how to move (by a page, by some distance...).
     */
    open fun moveDown() {
        moveRight()
    }

    /**
     * Moves pages up. Implementations decide how to move (by a page, by some distance...).
     */
    open fun moveUp() {
        moveLeft()
    }

    /**
     * Method the implementations can call to show a menu with options for the given page.
     */
    fun onLongClick(page: Page?): Boolean {
        if (isAdded && page != null) {
            readerActivity.onLongClick(page)
        }
        return true
    }

    /**
     * Sets the active decoder class.
     *
     * @param value the decoder class to use.
     */
    fun setDecoderClass(value: Int) {
        when (value) {
            IMAGE_DECODER -> {
                bitmapDecoderClass = IImageDecoder::class.java
                regionDecoderClass = IImageRegionDecoder::class.java
            }
            RAPID_DECODER -> {
                bitmapDecoderClass = RapidImageDecoder::class.java
                regionDecoderClass = RapidImageRegionDecoder::class.java
            }
            SKIA_DECODER -> {
                bitmapDecoderClass = SkiaImageDecoder::class.java
                regionDecoderClass = SkiaImageRegionDecoder::class.java
            }
        }
    }

    /**
     * Property to get the reader activity.
     */
    val readerActivity: ReaderActivity
        get() = activity as ReaderActivity

}
