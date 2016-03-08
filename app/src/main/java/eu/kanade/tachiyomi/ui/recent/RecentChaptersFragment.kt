package eu.kanade.tachiyomi.ui.recent

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.decoration.DividerItemDecoration
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.setInformationDrawable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_recent_chapters.*
import nucleus.factory.RequiresPresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * Fragment that shows recent chapters.
 * Uses R.layout.fragment_recent_chapters.
 * UI related actions should be called from here.
 */
@RequiresPresenter(RecentChaptersPresenter::class)
class RecentChaptersFragment : BaseRxFragment<RecentChaptersPresenter>(), FlexibleViewHolder.OnListItemClickListener {
    companion object {
        /**
         * Create new RecentChaptersFragment.
         *
         */
        @JvmStatic
        fun newInstance(): RecentChaptersFragment {
            return RecentChaptersFragment()
        }
    }

    /**
     * Adapter containing the recent chapters.
     */
    lateinit var adapter: RecentChaptersAdapter
        private set

    /**
     * Called when view gets created
     *
     * @param inflater layout inflater
     * @param container view group
     * @param savedState status of saved state
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        // Inflate view
        return inflater.inflate(R.layout.fragment_recent_chapters, container, false)
    }

    /**
     * Called when view is created
     *
     * @param view created view
     * @param savedInstanceState status of saved sate
     */
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        // Init RecyclerView and adapter
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.addItemDecoration(DividerItemDecoration(ContextCompat.getDrawable(
                context, R.drawable.line_divider)))
        recycler.setHasFixedSize(true)
        adapter = RecentChaptersAdapter(this)
        recycler.adapter = adapter

        // Update toolbar text
        setToolbarTitle(R.string.label_recent_updates)

        // Check if recent chapters is empty and update information accordingly.
        (activity as MainActivity).image_view.setInformationDrawable(R.drawable.ic_history_grey_128dp)
        (activity as MainActivity).text_label.text = getString(R.string.information_no_recent)
        (activity as MainActivity).information_layout.bringToFront()
    }

    /**
     * Called when item in list is clicked
     *
     * @param position position of clicked item
     */
    override fun onListItemClick(position: Int): Boolean {
        // Get item from position
        val item = adapter.getItem(position)
        if (item is MangaChapter) {
            // Open chapter in reader
            openChapter(item)
        }
        return false
    }

    /**
     * Called when item in list is long clicked
     *
     * @param position position of clicked item
     */
    override fun onListItemLongClick(position: Int) {
        // Empty function
    }

    /**
     * Open chapter in reader

     * @param chapter selected chapter
     */
    private fun openChapter(chapter: MangaChapter) {
        // Start reader event
        presenter.onOpenChapter(chapter)

        //Start reader intent
        val intent = ReaderActivity.newIntent(activity)
        startActivity(intent)
    }

    /**
     * Populate adapter with chapters

     * @param chapters list of chapters
     */
    fun onNextMangaChapters(chapters: List<Any>) {
        if (!chapters.isEmpty()) {
            ( activity as MainActivity).image_view.setInformationDrawable(null)
            ( activity as MainActivity).text_label.text = ""
        }
        adapter.setItems(chapters)
    }

    /**
     * Update download status of chapter

     * @param download download object containing download progress.
     */
    fun onChapterStatusChange(download: Download) {
        getHolder(download)?.onStatusChange(download.status)

    }

    /**
     * Returns holder belonging to chapter
     *
     * @param download download object containing download progress.
     */
    private fun getHolder(download: Download): RecentChaptersHolder? {
        return recycler.findViewHolderForItemId(download.chapter.id) as? RecentChaptersHolder
    }

    /**
     * Start downloading chapter

     * @param chapters selected chapters
     * @param manga    manga that belongs to chapter
     * @return true
     */
    fun onDownload(chapters: Observable<Chapter>, manga: Manga): Boolean {
        // Start the download service.
        DownloadService.start(activity)

        // Refresh data on download competition.
        val observable = chapters
                .doOnCompleted({
                    adapter.notifyDataSetChanged()
                    presenter.start(presenter.CHAPTER_STATUS_CHANGES)
                })

        // Download chapter.
        presenter.downloadChapter(observable, manga)
        return true
    }

    /**
     * Start deleting chapter
     *
     * @param chapters selected chapters
     * @param manga manga that belongs to chapter
     * @return success of deletion.
     */
    fun onDelete(chapters: Observable<Chapter>, manga: Manga): Boolean {
        //Create observable
        val observable = chapters
                .concatMap { chapter ->
                    presenter.deleteChapter(chapter, manga)
                    Observable.just(chapter)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { chapter ->
                    chapter.status = Download.NOT_DOWNLOADED
                }
                .doOnCompleted { adapter.notifyDataSetChanged() }

        // Delete chapters with observable
        presenter.deleteChapters(observable)

        return true
    }

    /**
     * Mark chapter as read

     * @param chapters selected chapter
     * @return true
     */
    fun onMarkAsRead(chapters: Observable<Chapter>): Boolean {
        // Set marked as read
        presenter.markChaptersRead(chapters, true)
        return true
    }

    /**
     * Mark chapter as unread

     * @param chapters selected chapter
     * @return true
     */
    fun onMarkAsUnread(chapters: Observable<Chapter>): Boolean {
        // Set marked as unread
        presenter.markChaptersRead(chapters, false)
        return true
    }


}