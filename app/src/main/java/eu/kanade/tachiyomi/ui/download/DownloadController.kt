package eu.kanade.tachiyomi.ui.download

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.databinding.DownloadControllerBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.base.controller.FabController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.util.view.shrinkOnScroll
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

/**
 * Controller that shows the currently active downloads.
 * Uses R.layout.fragment_download_queue.
 */
class DownloadController :
    NucleusController<DownloadControllerBinding, DownloadPresenter>(),
    FabController,
    DownloadAdapter.DownloadItemListener {

    /**
     * Adapter containing the active downloads.
     */
    private var adapter: DownloadAdapter? = null

    private var actionFab: ExtendedFloatingActionButton? = null
    private var actionFabScrollListener: RecyclerView.OnScrollListener? = null

    /**
     * Map of subscriptions for active downloads.
     */
    private val progressSubscriptions by lazy { mutableMapOf<Download, Subscription>() }

    /**
     * Whether the download queue is running or not.
     */
    private var isRunning: Boolean = false

    init {
        setHasOptionsMenu(true)
    }

    override fun createBinding(inflater: LayoutInflater) = DownloadControllerBinding.inflate(inflater)

    override fun createPresenter(): DownloadPresenter {
        return DownloadPresenter()
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_download_queue)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        // Check if download queue is empty and update information accordingly.
        setInformationView()

        // Initialize adapter.
        adapter = DownloadAdapter(this@DownloadController)
        binding.recycler.adapter = adapter
        adapter?.isHandleDragEnabled = true
        adapter?.fastScroller = binding.fastScroller

        // Set the layout manager for the recycler and fixed size.
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.setHasFixedSize(true)

        actionFabScrollListener = actionFab?.shrinkOnScroll(binding.recycler)

        // Subscribe to changes
        DownloadService.runningRelay
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { onQueueStatusChange(it) }

        presenter.getDownloadStatusObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { onStatusChange(it) }

        presenter.getDownloadProgressObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { onUpdateDownloadedPages(it) }
    }

    override fun configureFab(fab: ExtendedFloatingActionButton) {
        actionFab = fab
        fab.setOnClickListener {
            val context = applicationContext ?: return@setOnClickListener

            if (isRunning) {
                DownloadService.stop(context)
                presenter.pauseDownloads()
            } else {
                DownloadService.start(context)
            }

            setInformationView()
        }
    }

    override fun cleanupFab(fab: ExtendedFloatingActionButton) {
        fab.setOnClickListener(null)
        actionFabScrollListener?.let { binding.recycler.removeOnScrollListener(it) }
        actionFab = null
    }

    override fun onDestroyView(view: View) {
        for (subscription in progressSubscriptions.values) {
            subscription.unsubscribe()
        }
        progressSubscriptions.clear()
        adapter = null
        super.onDestroyView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.download_queue, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.clear_queue).isVisible = !presenter.downloadQueue.isEmpty()
        menu.findItem(R.id.reorder).isVisible = !presenter.downloadQueue.isEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val context = applicationContext ?: return false
        when (item.itemId) {
            R.id.clear_queue -> {
                DownloadService.stop(context)
                presenter.clearQueue()
            }
            R.id.newest, R.id.oldest -> {
                reorderQueue({ it.download.chapter.date_upload }, item.itemId == R.id.newest)
            }
            R.id.asc, R.id.desc -> {
                reorderQueue({ it.download.chapter.chapter_number }, item.itemId == R.id.desc)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun <R : Comparable<R>> reorderQueue(selector: (DownloadItem) -> R, reverse: Boolean = false) {
        val adapter = adapter ?: return
        val items = adapter.currentItems.sortedBy(selector).toMutableList()
        if (reverse) {
            items.reverse()
        }
        adapter.updateDataSet(items)
        val downloads = items.mapNotNull { it.download }
        presenter.reorder(downloads)
    }

    /**
     * Called when the status of a download changes.
     *
     * @param download the download whose status has changed.
     */
    private fun onStatusChange(download: Download) {
        when (download.status) {
            Download.State.DOWNLOADING -> {
                observeProgress(download)
                // Initial update of the downloaded pages
                onUpdateDownloadedPages(download)
            }
            Download.State.DOWNLOADED -> {
                unsubscribeProgress(download)
                onUpdateProgress(download)
                onUpdateDownloadedPages(download)
            }
            Download.State.ERROR -> unsubscribeProgress(download)
            else -> { /* unused */ }
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

        progressSubscriptions[download] = subscription
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
        activity?.invalidateOptionsMenu()

        // Check if download queue is empty and update information accordingly.
        setInformationView()
    }

    /**
     * Called from the presenter to assign the downloads for the adapter.
     *
     * @param downloads the downloads from the queue.
     */
    fun onNextDownloads(downloads: List<DownloadItem>) {
        activity?.invalidateOptionsMenu()
        setInformationView()
        adapter?.updateDataSet(downloads)
    }

    /**
     * Called when the progress of a download changes.
     *
     * @param download the download whose progress has changed.
     */
    private fun onUpdateProgress(download: Download) {
        getHolder(download)?.notifyProgress()
    }

    /**
     * Called when a page of a download is downloaded.
     *
     * @param download the download whose page has been downloaded.
     */
    private fun onUpdateDownloadedPages(download: Download) {
        getHolder(download)?.notifyDownloadedPages()
    }

    /**
     * Returns the holder for the given download.
     *
     * @param download the download to find.
     * @return the holder of the download or null if it's not bound.
     */
    private fun getHolder(download: Download): DownloadHolder? {
        return binding.recycler.findViewHolderForItemId(download.chapter.id!!) as? DownloadHolder
    }

    /**
     * Set information view when queue is empty
     */
    private fun setInformationView() {
        if (presenter.downloadQueue.isEmpty()) {
            binding.emptyView.show(R.string.information_no_downloads)
            actionFab?.isVisible = false
        } else {
            binding.emptyView.hide()
            actionFab?.apply {
                isVisible = true

                setText(
                    if (isRunning) {
                        R.string.action_pause
                    } else {
                        R.string.action_resume
                    }
                )

                setIconResource(
                    if (isRunning) {
                        R.drawable.ic_pause_24dp
                    } else {
                        R.drawable.ic_play_arrow_24dp
                    }
                )
            }
        }
    }

    /**
     * Called when an item is released from a drag.
     *
     * @param position The position of the released item.
     */
    override fun onItemReleased(position: Int) {
        val adapter = adapter ?: return
        val downloads = (0 until adapter.itemCount).mapNotNull { adapter.getItem(it)?.download }
        presenter.reorder(downloads)
    }

    /**
     * Called when the menu item of a download is pressed
     *
     * @param position The position of the item
     * @param menuItem The menu Item pressed
     */
    override fun onMenuItemClick(position: Int, menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.move_to_top, R.id.move_to_bottom -> {
                val download = adapter?.getItem(position) ?: return
                val items = adapter?.currentItems?.toMutableList() ?: return
                items.remove(download)
                if (menuItem.itemId == R.id.move_to_top) {
                    items.add(0, download)
                } else {
                    items.add(download)
                }

                val adapter = adapter ?: return
                adapter.updateDataSet(items)
                val downloads = adapter.currentItems.mapNotNull { it?.download }
                presenter.reorder(downloads)
            }
            R.id.cancel_download -> {
                val download = adapter?.getItem(position)?.download ?: return
                presenter.cancelDownload(download)

                val adapter = adapter ?: return
                adapter.removeItem(position)
                val downloads = adapter.currentItems.mapNotNull { it?.download }
                presenter.reorder(downloads)
            }
            R.id.cancel_series -> {
                val download = adapter?.getItem(position)?.download ?: return
                val allDownloadsForSeries = adapter?.currentItems
                    ?.filter { download.manga.id == it.download.manga.id }
                    ?.map(DownloadItem::download)
                if (!allDownloadsForSeries.isNullOrEmpty()) {
                    presenter.cancelDownloads(allDownloadsForSeries)
                }
            }
        }
    }
}
