package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.support.v7.widget.RecyclerView
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.base.PageDecodeErrorLayout
import kotlinx.android.synthetic.main.chapter_image.view.*
import kotlinx.android.synthetic.main.item_webtoon_reader.view.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import java.io.File

/**
 * Holder for webtoon reader for a single page of a chapter.
 * All the elements from the layout file "item_webtoon_reader" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new webtoon holder.
 */
class WebtoonHolder(private val view: View, private val adapter: WebtoonAdapter) :
        RecyclerView.ViewHolder(view) {

    /**
     * Page of a chapter.
     */
    private var page: Page? = null

    /**
     * Subscription for status changes of the page.
     */
    private var statusSubscription: Subscription? = null

    /**
     * Layout of decode error.
     */
    private var decodeErrorLayout: PageDecodeErrorLayout? = null

    init {
        with(view.image_view) {
            setMaxBitmapDimensions(readerActivity.maxBitmapSize)
            setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_FIXED)
            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
            setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_FIT_WIDTH)
            maxScale = 10f
            setRegionDecoderClass(webtoonReader.regionDecoderClass)
            setBitmapDecoderClass(webtoonReader.bitmapDecoderClass)
            setVerticalScrollingParent(true)
            setOnTouchListener(adapter.touchListener)
            setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                override fun onImageLoaded() {
                    // When the image is loaded, reset the minimum height to avoid gaps
                    view.frame_container.minimumHeight = 30
                }

                override fun onImageLoadError(e: Exception) {
                    onImageDecodeError()
                }
            })
        }

        // Avoid to create a lot of view holders taking twice the screen height,
        // saving memory and a possible OOM. When the first image is loaded in this holder,
        // the minimum size will be removed.
        // Doing this we get sequential holder instantiation.
        view.frame_container.minimumHeight = view.resources.displayMetrics.heightPixels * 2

        // Leave some space between progress bars
        view.progress.minimumHeight = 300

        view.frame_container.setOnTouchListener(adapter.touchListener)
        view.retry_button.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                readerActivity.presenter.retryPage(page)
            }
            true
        }
    }

    /**
     * Method called from [WebtoonAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given page.
     *
     * @param page the page to bind.
     */
    fun onSetValues(page: Page) {
        decodeErrorLayout?.let {
            (view as ViewGroup).removeView(it)
            decodeErrorLayout = null
        }

        this.page = page
        observeStatus()
    }

    /**
     * Observes the status of the page and notify the changes.
     *
     * @see processStatus
     */
    private fun observeStatus() {
        page?.let { page ->
            val statusSubject = SerializedSubject(PublishSubject.create<Int>())
            page.setStatusSubject(statusSubject)

            statusSubscription?.unsubscribe()
            statusSubscription = statusSubject.startWith(page.status)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { processStatus(it) }

            webtoonReader.subscriptions.add(statusSubscription)
        }
    }

    /**
     * Called when the status of the page changes.
     *
     * @param status the new status of the page.
     */
    private fun processStatus(status: Int) {
        when (status) {
            Page.QUEUE -> onQueue()
            Page.LOAD_PAGE -> onLoading()
            Page.DOWNLOAD_IMAGE -> onLoading()
            Page.READY -> onReady()
            Page.ERROR -> onError()
        }
    }

    /**
     * Unsubscribes from the status subscription.
     */
    fun unsubscribeStatus() {
        statusSubscription?.unsubscribe()
        statusSubscription = null
    }

    /**
     * Called when the page is loading.
     */
    private fun onLoading() {
        setRetryButtonVisible(false)
        setImageVisible(false)
        setProgressVisible(true)
    }

    /**
     * Called when the page is ready.
     */
    private fun onReady() {
        setRetryButtonVisible(false)
        setProgressVisible(false)
        setImageVisible(true)

        page?.imagePath?.let { path ->
            if (File(path).exists()) {
                view.image_view.setImage(ImageSource.uri(path))
                view.progress.visibility = View.GONE
            } else {
                page?.status = Page.ERROR
            }
        }
    }

    /**
     * Called when the page has an error.
     */
    private fun onError() {
        setImageVisible(false)
        setProgressVisible(false)
        setRetryButtonVisible(true)
    }

    /**
     * Called when the page is queued.
     */
    private fun onQueue() {
        setImageVisible(false)
        setRetryButtonVisible(false)
        setProgressVisible(false)
    }

    /**
     * Called when the image fails to decode.
     */
    private fun onImageDecodeError() {
        page?.let { page ->
            decodeErrorLayout = PageDecodeErrorLayout(view.context, page, readerActivity.readerTheme,
                    { readerActivity.presenter.retryPage(page) })

            (view as ViewGroup).addView(decodeErrorLayout)
        }
    }

    /**
     * Sets the visibility of the progress bar.
     *
     * @param visible whether to show it or not.
     */
    private fun setProgressVisible(visible: Boolean) {
        view.progress.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Sets the visibility of the image view.
     *
     * @param visible whether to show it or not.
     */
    private fun setImageVisible(visible: Boolean) {
        view.image_view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Sets the visibility of the retry button.
     *
     * @param visible whether to show it or not.
     */
    private fun setRetryButtonVisible(visible: Boolean) {
        view.retry_button.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Property to get the reader activity.
     */
    private val readerActivity: ReaderActivity
        get() = adapter.fragment.readerActivity

    /**
     * Property to get the webtoon reader.
     */
    private val webtoonReader: WebtoonReader
        get() = adapter.fragment
}