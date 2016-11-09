package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.content.Context
import android.graphics.PointF
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.base.PageDecodeErrorLayout
import eu.kanade.tachiyomi.ui.reader.viewer.pager.horizontal.RightToLeftReader
import eu.kanade.tachiyomi.ui.reader.viewer.pager.vertical.VerticalReader
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.chapter_image.view.*
import kotlinx.android.synthetic.main.item_pager_reader.view.*
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import java.util.concurrent.TimeUnit

class PageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
: FrameLayout(context, attrs) {

    /**
     * Page of a chapter.
     */
    lateinit var page: Page

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

    fun initialize(reader: PagerReader, page: Page) {
        val activity = reader.activity as ReaderActivity

        when (activity.readerTheme) {
            ReaderActivity.BLACK_THEME -> progress_text.setTextColor(reader.whiteColor)
            ReaderActivity.WHITE_THEME -> progress_text.setTextColor(reader.blackColor)
        }

        if (reader is RightToLeftReader) {
            rotation = -180f
        }

        with(image_view) {
            setMaxBitmapDimensions((reader.activity as ReaderActivity).maxBitmapSize)
            setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_FIXED)
            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
            setMinimumScaleType(reader.scaleType)
            setMinimumDpi(90)
            setMinimumTileDpi(180)
            setRegionDecoderClass(reader.regionDecoderClass)
            setBitmapDecoderClass(reader.bitmapDecoderClass)
            setVerticalScrollingParent(reader is VerticalReader)
            setOnTouchListener { v, motionEvent -> reader.gestureDetector.onTouchEvent(motionEvent) }
            setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                override fun onReady() {
                    onImageDecoded(reader)
                }

                override fun onImageLoadError(e: Exception) {
                    onImageDecodeError(reader)
                }
            })
        }

        retry_button.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                activity.presenter.retryPage(page)
            }
            true
        }

        this.page = page
        observeStatus()
    }

    override fun onDetachedFromWindow() {
        unsubscribeProgress()
        unsubscribeStatus()
        image_view.setOnTouchListener(null)
        image_view.setOnImageEventListener(null)
        super.onDetachedFromWindow()
    }

    /**
     * Observes the status of the page and notify the changes.
     *
     * @see processStatus
     */
    private fun observeStatus() {
        statusSubscription?.unsubscribe()

        val statusSubject = SerializedSubject(PublishSubject.create<Int>())
        page.setStatusSubject(statusSubject)

        statusSubscription = statusSubject.startWith(page.status)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { processStatus(it) }
    }

    /**
     * Observes the progress of the page and updates view.
     */
    private fun observeProgress() {
        progressSubscription?.unsubscribe()

        progressSubscription = Observable.interval(100, TimeUnit.MILLISECONDS)
                .map { page.progress }
                .distinctUntilChanged()
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { progress ->
                    progress_text.text = context.getString(R.string.download_progress, progress)
                }
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
     * Unsubscribes from the status subscription.
     */
    private fun unsubscribeStatus() {
        page.setStatusSubject(null)
        statusSubscription?.unsubscribe()
        statusSubscription = null
    }

    /**
     * Unsubscribes from the progress subscription.
     */
    private fun unsubscribeProgress() {
        progressSubscription?.unsubscribe()
        progressSubscription = null
    }

    /**
     * Called when the page is queued.
     */
    private fun setQueued() {
        progress_container.visibility = View.VISIBLE
        progress_text.visibility = View.INVISIBLE
        retry_button.visibility = View.GONE
        decodeErrorLayout?.let {
            removeView(it)
            decodeErrorLayout = null
        }
    }

    /**
     * Called when the page is loading.
     */
    private fun setLoading() {
        progress_container.visibility = View.VISIBLE
        progress_text.visibility = View.VISIBLE
        progress_text.setText(R.string.downloading)
    }

    /**
     * Called when the page is downloading.
     */
    private fun setDownloading() {
        progress_container.visibility = View.VISIBLE
        progress_text.visibility = View.VISIBLE
    }

    /**
     * Called when the page is ready.
     */
    private fun setImage() {
        var path = page.imagePath
        if (path == null) {
            page.status = Page.ERROR
            return
        }

        // FIXME use Uri instead
        if (!path.contains("://")) {
            if (path.startsWith("/")) {
                path = path.substring(1)
            }
            path = "file:///" + path
        }

        val file = UniFile.fromUri(context, Uri.parse(path))
        if (!file.exists()) {
            page.status = Page.ERROR
            return
        }

        progress_text.visibility = View.INVISIBLE
        image_view.setImage(ImageSource.uri(file.uri))
    }

    /**
     * Called when the page has an error.
     */
    private fun setError() {
        progress_container.visibility = View.GONE
        retry_button.visibility = View.VISIBLE
    }

    /**
     * Called when the image is decoded and going to be displayed.
     */
    private fun onImageDecoded(reader: PagerReader) {
        progress_container.visibility = View.GONE

        with(image_view) {
            when (reader.zoomType) {
                PagerReader.ALIGN_LEFT -> setScaleAndCenter(scale, PointF(0f, 0f))
                PagerReader.ALIGN_RIGHT -> setScaleAndCenter(scale, PointF(sWidth.toFloat(), 0f))
                PagerReader.ALIGN_CENTER -> setScaleAndCenter(scale, center.apply { y = 0f })
            }
        }
    }

    /**
     * Called when an image fails to decode.
     */
    private fun onImageDecodeError(reader: PagerReader) {
        progress_container.visibility = View.GONE

        if (decodeErrorLayout != null || !reader.isAdded) return

        val activity = reader.activity as ReaderActivity

        val layout = inflate(R.layout.page_decode_error)
        PageDecodeErrorLayout(layout, page, activity.readerTheme, {
            if (reader.isAdded) {
                activity.presenter.retryPage(page)
            }
        })
        decodeErrorLayout = layout
        addView(layout)
    }

}