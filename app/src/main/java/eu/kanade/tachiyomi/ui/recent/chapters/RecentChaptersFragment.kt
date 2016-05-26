package eu.kanade.tachiyomi.ui.recent.chapters

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.decoration.DividerItemDecoration
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.getResourceDrawable
import eu.kanade.tachiyomi.widget.DeletingChaptersDialog
import eu.kanade.tachiyomi.widget.NpaLinearLayoutManager
import kotlinx.android.synthetic.main.fragment_recent_chapters.*
import nucleus.factory.RequiresPresenter
import timber.log.Timber

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
        recycler.layoutManager = NpaLinearLayoutManager(activity)
        recycler.addItemDecoration(DividerItemDecoration(context.theme.getResourceDrawable(R.attr.divider_drawable)))
        recycler.setHasFixedSize(true)
        adapter = RecentChaptersAdapter(this)
        recycler.adapter = adapter

        // Update toolbar text
        setToolbarTitle(R.string.label_recent_updates)
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
     *
     * @param chapter selected chapter
     */
    private fun openChapter(chapter: MangaChapter) {
        val intent = ReaderActivity.newIntent(activity, chapter.manga, chapter.chapter)
        startActivity(intent)
    }

    /**
     * Populate adapter with chapters
     *
     * @param chapters list of chapters
     */
    fun onNextMangaChapters(chapters: List<Any>) {
        (activity as MainActivity).updateEmptyView(chapters.isEmpty(),
                R.string.information_no_recent, R.drawable.ic_update_black_128dp)

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
     * Mark chapter as read
     *
     * @param item selected chapter with manga
     */
    fun markAsRead(item: MangaChapter) {
        presenter.markChapterRead(item.chapter, true)
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            deleteChapter(item)
        }
    }

    /**
     * Mark chapter as unread
     *
     * @param item selected chapter with manga
     */
    fun markAsUnread(item: MangaChapter) {
        presenter.markChapterRead(item.chapter, false)
    }

    /**
     * Start downloading chapter
     *
     * @param item selected chapter with manga
     */
    fun downloadChapter(item: MangaChapter) {
        presenter.downloadChapter(item)
    }

    /**
     * Start deleting chapter
     *
     * @param item selected chapter with manga
     */
    fun deleteChapter(item: MangaChapter) {
        DeletingChaptersDialog().show(childFragmentManager, DeletingChaptersDialog.TAG)
        presenter.deleteChapter(item)
    }

    fun onChaptersDeleted() {
        dismissDeletingDialog()
        adapter.notifyDataSetChanged()
    }

    fun onChaptersDeletedError(error: Throwable) {
        dismissDeletingDialog()
        Timber.e(error, error.message)
    }

    fun dismissDeletingDialog() {
        (childFragmentManager.findFragmentByTag(DeletingChaptersDialog.TAG) as? DialogFragment)?.dismiss()
    }

}