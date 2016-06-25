package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.PointF
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.ui.base.fragment.BaseFragment
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.base.PageDecodeErrorLayout
import eu.kanade.tachiyomi.ui.reader.viewer.pager.horizontal.RightToLeftReader
import eu.kanade.tachiyomi.ui.reader.viewer.pager.vertical.VerticalReader
import kotlinx.android.synthetic.main.chapter_image.*
import kotlinx.android.synthetic.main.item_pager_reader.*
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fragment for a single page of the ViewPager reader.
 * All the elements from the layout file "item_pager_reader" are available in this class.
 */
class PagerReaderFragment : BaseFragment() {

    companion object {
        /**
         * Creates a new instance of this fragment.
         *
         * @return a new instance of [PagerReaderFragment].
         */
        fun newInstance(): PagerReaderFragment {
            return PagerReaderFragment()
        }
    }

    /**
     * Page of a chapter.
     */
    var page: Page? = null
        set(value) {
            field = value
            // Observe status if the view is initialized
            if (view != null) {
                observeStatus()
            }
        }

    /**
     * Position of the fragment in the adapter.
     */
    var position = -1

    /**
     * Subscription for progress changes of the page.
     */
    private var progressSubscription: Subscription? = null

    /**
     * Subscription for status changes of the page.
     */
    private var statusSubscription: Subscription? = null

    /**
     * Text color for black theme.
     */
    private val whiteColor by lazy { ContextCompat.getColor(context, R.color.textColorSecondaryDark) }

    /**
     * Text color for white theme.
     */
    private val blackColor by lazy { ContextCompat.getColor(context, R.color.textColorSecondaryLight) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.item_pager_reader, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        if (readerActivity.readerTheme == ReaderActivity.BLACK_THEME) {
            progress_text.setTextColor(whiteColor)
        } else {
            progress_text.setTextColor(blackColor)
        }

        if (pagerReader is RightToLeftReader) {
            view.rotation = -180f
        }

        with(image_view) {
            setMaxBitmapDimensions(readerActivity.maxBitmapSize)
            setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_FIXED)
            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
            setMinimumScaleType(pagerReader.scaleType)
            setMinimumDpi(50)
            setRegionDecoderClass(pagerReader.regionDecoderClass)
            setBitmapDecoderClass(pagerReader.bitmapDecoderClass)
            setVerticalScrollingParent(pagerReader is VerticalReader)
            setOnTouchListener { v, motionEvent -> pagerReader.gestureDetector.onTouchEvent(motionEvent) }
            setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                override fun onReady() {
                    when (pagerReader.zoomType) {
                        PagerReader.ALIGN_LEFT -> setScaleAndCenter(scale, PointF(0f, 0f))
                        PagerReader.ALIGN_RIGHT -> setScaleAndCenter(scale, PointF(sWidth.toFloat(), 0f))
                        PagerReader.ALIGN_CENTER -> {
                            val newCenter = center
                            newCenter.y = 0f
                            setScaleAndCenter(scale, newCenter)
                        }
                    }
                }

                override fun onImageLoadError(e: Exception) {
                    onImageDecodeError()
                }
            })
        }

        retry_button.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                readerActivity.presenter.retryPage(page)
            }
            true
        }

        observeStatus()
    }

    override fun onDestroyView() {
        unsubscribeProgress()
        unsubscribeStatus()
        image_view.setOnTouchListener(null)
        image_view.setOnImageEventListener(null)
        super.onDestroyView()
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
        }
    }

    /**
     * Observes the progress of the page and updates view.
     */
    private fun observeProgress() {
        val currentValue = AtomicInteger(-1)

        progressSubscription?.unsubscribe()
        progressSubscription = Observable.interval(100, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // Refresh UI only if progress change
                    if (page?.progress != currentValue.get()) {
                        currentValue.set(page?.progress ?: 0)
                        progress_text.text = getString(R.string.download_progress, currentValue.get())
                    }
                }
    }

    /**
     * Called when the status of the page changes.
     *
     * @param status the new status of the page.
     */
    private fun processStatus(status: Int) {
        when (status) {
            Page.QUEUE -> hideError()
            Page.LOAD_PAGE -> onLoading()
            Page.DOWNLOAD_IMAGE -> {
                observeProgress()
                onDownloading()
            }
            Page.READY -> {
                onReady()
                unsubscribeProgress()
            }
            Page.ERROR -> {
                onError()
                unsubscribeProgress()
            }
        }
    }

    /**
     * Unsubscribes from the status subscription.
     */
    private fun unsubscribeStatus() {
        page?.setStatusSubject(null)
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
     * Called when the page is loading.
     */
    private fun onLoading() {
        progress_container.visibility = View.VISIBLE
        progress_text.visibility = View.VISIBLE
        progress_text.setText(R.string.downloading)
    }

    /**
     * Called when the page is downloading.
     */
    private fun onDownloading() {
        progress_container.visibility = View.VISIBLE
        progress_text.visibility = View.VISIBLE
    }

    /**
     * Called when the page is ready.
     */
    private fun onReady() {
        page?.imagePath?.let { path ->
            if (File(path).exists()) {
                image_view.setImage(ImageSource.uri(path))
                progress_container.visibility = View.GONE
            } else {
                page?.status = Page.ERROR
            }
        }
    }

    /**
     * Called when the page has an error.
     */
    private fun onError() {
        progress_container.visibility = View.GONE
        retry_button.visibility = View.VISIBLE
    }

    /**
     * Hides the error layout.
     */
    private fun hideError() {
        retry_button.visibility = View.GONE
    }

    /**
     * Called when an image fails to decode.
     */
    private fun onImageDecodeError() {
        val view = view as? ViewGroup ?: return

        page?.let { page ->
            val errorLayout = PageDecodeErrorLayout(context, page, readerActivity.readerTheme,
                    { readerActivity.presenter.retryPage(page) })

            view.addView(errorLayout)
        }
    }

    /**
     * Property to get the reader activity.
     */
    private val readerActivity: ReaderActivity
        get() = activity as ReaderActivity

    /**
     * Property to get the pager reader.
     */
    private val pagerReader: PagerReader
        get() = parentFragment as PagerReader

}
