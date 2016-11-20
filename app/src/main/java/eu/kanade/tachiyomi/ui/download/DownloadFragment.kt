package eu.kanade.tachiyomi.ui.download

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.plusAssign
import kotlinx.android.synthetic.main.fragment_download_queue.*
import nucleus.factory.RequiresPresenter
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Fragment that shows the currently active downloads.
 * Uses R.layout.fragment_download_queue.
 */
@RequiresPresenter(DownloadPresenter::class)
class DownloadFragment : BaseRxFragment<DownloadPresenter>() {

    /**
     * Adapter containing the active downloads.
     */
    private lateinit var adapter: DownloadAdapter

    /**
     * Subscription list to be cleared during [onDestroyView].
     */
    private val subscriptions by lazy { CompositeSubscription() }

    /**
     * Map of subscriptions for active downloads.
     */
    private val progressSubscriptions by lazy { HashMap<Download, Subscription>() }

    /**
     * Whether the download queue is running or not.
     */
    private var isRunning: Boolean = false

    companion object {
        /**
         * Creates a new instance of this fragment.
         *
         * @return a new instance of [DownloadFragment].
         */
        fun newInstance(): DownloadFragment {
            return DownloadFragment()
        }
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_download_queue, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        setToolbarTitle(R.string.label_download_queue)

        // Check if download queue is empty and update information accordingly.
        setInformationView()

        // Initialize adapter.
        adapter = DownloadAdapter(activity)
        recycler.adapter = adapter

        // Set the layout manager for the recycler and fixed size.
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.setHasFixedSize(true)

        // Suscribe to changes
        subscriptions += DownloadService.runningRelay
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onQueueStatusChange(it) }

        subscriptions += presenter.getDownloadStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onStatusChange(it) }

        subscriptions += presenter.getDownloadProgressObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onUpdateDownloadedPages(it) }
    }

    override fun onDestroyView() {
        for (subscription in progressSubscriptions.values) {
            subscription.unsubscribe()
        }
        progressSubscriptions.clear()
        subscriptions.clear()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.download_queue, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        // Set start button visibility.
        menu.findItem(R.id.start_queue).isVisible = !isRunning && !presenter.downloadQueue.isEmpty()

        // Set pause button visibility.
        menu.findItem(R.id.pause_queue).isVisible = isRunning

        // Set clear button visibility.
        menu.findItem(R.id.clear_queue).isVisible = !presenter.downloadQueue.isEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.start_queue -> DownloadService.start(activity)
            R.id.pause_queue -> DownloadService.stop(activity)
            R.id.clear_queue -> {
                DownloadService.stop(activity)
                presenter.clearQueue()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Called when the status of a download changes.
     *
     * @param download the download whose status has changed.
     */
    private fun onStatusChange(download: Download) {
        when (download.status) {
            Download.DOWNLOADING -> {
                observeProgress(download)
                // Initial update of the downloaded pages
                onUpdateDownloadedPages(download)
            }
            Download.DOWNLOADED -> {
                unsubscribeProgress(download)
                onUpdateProgress(download)
                onUpdateDownloadedPages(download)
            }
            Download.ERROR -> unsubscribeProgress(download)
        }
    }

    /**
     * Observe the progress of a download and notify the view.
     *
     * @param download the download to observe its progress.
     */
    private fun observeProgress(download: Download) {
        val subscription = Observable.interval(50, TimeUnit.MILLISECONDS)
                // Get the sum of percentages for all the pages.
                .flatMap {
                    Observable.from(download.pages)
                            .map(Page::progress)
                            .reduce { x, y -> x + y }
                }
                // Keep only the latest emission to avoid backpressure.
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { progress ->
                    // Update the view only if the progress has changed.
                    if (download.totalProgress != progress) {
                        download.totalProgress = progress
                        onUpdateProgress(download)
                    }
                }

        // Avoid leaking subscriptions
        progressSubscriptions.remove(download)?.unsubscribe()

        progressSubscriptions.put(download, subscription)
    }

    /**
     * Unsubscribes the given download from the progress subscriptions.
     *
     * @param download the download to unsubscribe.
     */
    private fun unsubscribeProgress(download: Download) {
        progressSubscriptions.remove(download)?.unsubscribe()
    }

    /**
     * Called when the queue's status has changed. Updates the visibility of the buttons.
     *
     * @param running whether the queue is now running or not.
     */
    private fun onQueueStatusChange(running: Boolean) {
        isRunning = running
        activity.supportInvalidateOptionsMenu()

        // Check if download queue is empty and update information accordingly.
        setInformationView()
    }

    /**
     * Called from the presenter to assign the downloads for the adapter.
     *
     * @param downloads the downloads from the queue.
     */
    fun onNextDownloads(downloads: List<Download>) {
        activity.supportInvalidateOptionsMenu()
        setInformationView()
        adapter.setItems(downloads)
    }

    /**
     * Called when the progress of a download changes.
     *
     * @param download the download whose progress has changed.
     */
    fun onUpdateProgress(download: Download) {
        getHolder(download)?.notifyProgress()
    }

    /**
     * Called when a page of a download is downloaded.
     *
     * @param download the download whose page has been downloaded.
     */
    fun onUpdateDownloadedPages(download: Download) {
        getHolder(download)?.notifyDownloadedPages()
    }

    /**
     * Returns the holder for the given download.
     *
     * @param download the download to find.
     * @return the holder of the download or null if it's not bound.
     */
    private fun getHolder(download: Download): DownloadHolder? {
        return recycler.findViewHolderForItemId(download.chapter.id!!) as? DownloadHolder
    }

    /**
     * Set information view when queue is empty
     */
    private fun setInformationView() {
        (activity as MainActivity).updateEmptyView(presenter.downloadQueue.isEmpty(),
                R.string.information_no_downloads, R.drawable.ic_file_download_black_128dp)
    }

}
