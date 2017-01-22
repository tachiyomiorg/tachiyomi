package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.support.v7.widget.RecyclerView
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.base.PageDecodeErrorLayout
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.chapter_image.view.*
import kotlinx.android.synthetic.main.item_webtoon_reader.view.*
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import java.util.concurrent.TimeUnit

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
     * Subscription for progress changes of the page.
     */
    private var progressSubscription: Subscription? = null

    /**
     * Layout of decode error.
     */
    private var decodeErrorLayout: View? = null

    init {
        with(view.image_view) {
            setMaxTileSize(readerActivity.maxBitmapSize)
            setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_FIXED)
            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
            setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_FIT_WIDTH)
            setMinimumDpi(90)
            setMinimumTileDpi(180)
            setRegionDecoderClass(webtoonReader.regionDecoderClass)
            setBitmapDecoderClass(webtoonReader.bitmapDecoderClass)
            setVerticalScrollingParent(true)
            setOnTouchListener(adapter.touchListener)
            setOnLongClickListener { webtoonReader.onLongClick(page) }
            setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                override fun onReady() {
                    onImageDecoded()
                }

                override fun onImageLoadError(e: Exception) {
                    onImageDecodeError()
                }
            })
        }

        view.progress_container.minimumHeight = view.resources.displayMetrics.heightPixels * 2

        view.setOnTouchListener(adapter.touchListener)
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
        this.page = page
        observeStatus()
    }

    /**
     * Called when the view is recycled and added to the view pool.
     */
    fun onRecycle() {
        unsubscribeStatus()
        unsubscribeProgress()
        decodeErrorLayout?.let {
            (view as ViewGroup).removeView(it)
            decodeErrorLayout = null
        }
        view.image_view.recycle()
        view.image_view.visibility = View.GONE
        view.progress_container.visibility = View.VISIBLE
    }

    /**
     * Observes the status of the page and notify the changes.
     *
     * @see processStatus
     */
    private fun observeStatus() {
        unsubscribeStatus()

        val page = page ?: return

        val statusSubject = SerializedSubject(PublishSubject.create<Int>())
        page.setStatusSubject(statusSubject)

        statusSubscription = statusSubject.startWith(page.status)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { processStatus(it) }

        addSubscription(statusSubscription)
    }

    /**
     * Observes the progress of the page and updates view.
     */
    private fun observeProgress() {
        unsubscribeProgress()

        val page = page ?: return

        progressSubscription = Observable.interval(100, TimeUnit.MILLISECONDS)
                .map { page.progress }
                .distinctUntilChanged()
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { progress ->
                    view.progress_text.text = view.context.getString(R.string.download_progress, progress)
                }

        addSubscription(progressSubscription)
    }

    /**
     * Called when the status of the page changes.
     *
     * @param status the new status of the page.
     */
    private fun processStatus(status: Int) {
        when (status) {
            Page.QUEUE -> setQueued()
            Page.LOAD_PAGE -> setLoading()
            Page.DOWNLOAD_IMAGE -> {
                observeProgress()
                setDownloading()
            }
            Page.READY -> {
                setImage()
                unsubscribeProgress()
            }
            Page.ERROR -> {
                setError()
                unsubscribeProgress()
            }
        }
    }

    /**
     * Adds a subscription to a list of subscriptions that will automatically unsubscribe when the
     * activity or the reader is destroyed.
     */
    private fun addSubscription(subscription: Subscription?) {
        webtoonReader.subscriptions.add(subscription)
    }

    /**
     * Removes a subscription from the list of subscriptions.
     */
    private fun removeSubscription(subscription: Subscription?) {
        subscription?.let { webtoonReader.subscriptions.remove(it) }
    }

    /**
     * Unsubscribes from the status subscription.
     */
    private fun unsubscribeStatus() {
        page?.setStatusSubject(null)
        removeSubscription(statusSubscription)
        statusSubscription = null
    }

    /**
     * Unsubscribes from the progress subscription.
     */
    private fun unsubscribeProgress() {
        removeSubscription(progressSubscription)
        progressSubscription = null
    }

    /**
     * Called when the page is queued.
     */
    private fun setQueued() = with(view) {
        progress_container.visibility = View.VISIBLE
        progress_text.visibility = View.INVISIBLE
        retry_container.visibility = View.GONE
        decodeErrorLayout?.let {
            (view as ViewGroup).removeView(it)
            decodeErrorLayout = null
        }
    }

    /**
     * Called when the page is loading.
     */
    private fun setLoading() = with(view) {
        progress_container.visibility = View.VISIBLE
        progress_text.visibility = View.VISIBLE
        progress_text.setText(R.string.downloading)
    }

    /**
     * Called when the page is downloading
     */
    private fun setDownloading() = with(view) {
        progress_container.visibility = View.VISIBLE
        progress_text.visibility = View.VISIBLE
    }

    /**
     * Called when the page is ready.
     */
    private fun setImage() = with(view) {
        val uri = page?.uri
        if (uri == null) {
            page?.status = Page.ERROR
            return
        }

        val file = UniFile.fromUri(context, uri)
        if (!file.exists()) {
            page?.status = Page.ERROR
            return
        }

        progress_text.visibility = View.INVISIBLE
        image_view.visibility = View.VISIBLE
        image_view.setImage(ImageSource.uri(file.uri))
    }

    /**
     * Called when the page has an error.
     */
    private fun setError() = with(view) {
        progress_container.visibility = View.GONE
        retry_container.visibility = View.VISIBLE
    }

    /**
     * Called when the image is decoded and going to be displayed.
     */
    private fun onImageDecoded() {
        view.progress_container.visibility = View.GONE
    }

    /**
     * Called when the image fails to decode.
     */
    private fun onImageDecodeError() {
        view.progress_container.visibility = View.GONE

        val page = page ?: return
        if (decodeErrorLayout != null || !webtoonReader.isAdded) return

        val layout = (view as ViewGroup).inflate(R.layout.page_decode_error)
        PageDecodeErrorLayout(layout, page, readerActivity.readerTheme, {
            if (webtoonReader.isAdded) {
                readerActivity.presenter.retryPage(page)
            }
        })
        decodeErrorLayout = layout
        view.addView(layout)
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