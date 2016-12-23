package eu.kanade.tachiyomi.ui.recent_updates

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.view.ActionMode
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.DeletingChaptersDialog
import kotlinx.android.synthetic.main.fragment_recent_chapters.*
import nucleus.factory.RequiresPresenter
import timber.log.Timber

/**
 * Fragment that shows recent chapters.
 * Uses [R.layout.fragment_recent_chapters].
 * UI related actions should be called from here.
 */
@RequiresPresenter(RecentChaptersPresenter::class)
class RecentChaptersFragment
: BaseRxFragment<RecentChaptersPresenter>(), ActionMode.Callback, FlexibleViewHolder.OnListItemClickListener {

    companion object {
        /**
         * Create new RecentChaptersFragment.
         * @return a new instance of [RecentChaptersFragment].
         */
        fun newInstance(): RecentChaptersFragment {
            return RecentChaptersFragment()
        }
    }

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    /**
     * Adapter containing the recent chapters.
     */
    lateinit var adapter: RecentChaptersAdapter
        private set

    /**
     * Called when view gets created
     * @param inflater layout inflater
     * @param container view group
     * @param savedState status of saved state
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        // Inflate view
        return inflater.inflate(R.layout.fragment_recent_chapters, container, false)
    }

    /**
     * Called when view is created
     * @param view created view
     * @param savedState status of saved sate
     */
    override fun onViewCreated(view: View, savedState: Bundle?) {
        // Init RecyclerView and adapter
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recycler.setHasFixedSize(true)
        adapter = RecentChaptersAdapter(this)
        recycler.adapter = adapter

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recycler: RecyclerView, newState: Int) {
                // Disable swipe refresh when view is not at the top
                val firstPos = (recycler.layoutManager as LinearLayoutManager)
                        .findFirstCompletelyVisibleItemPosition()
                swipe_refresh.isEnabled = firstPos == 0
            }
        })

        swipe_refresh.setDistanceToTriggerSync((2 * 64 * resources.displayMetrics.density).toInt())
        swipe_refresh.setOnRefreshListener {
            if (!LibraryUpdateService.isRunning(activity)) {
                LibraryUpdateService.start(activity)
                context.toast(R.string.action_update_library)
            }
            // It can be a very long operation, so we disable swipe refresh and show a toast.
            swipe_refresh.isRefreshing = false
        }

        // Update toolbar text
        setToolbarTitle(R.string.label_recent_updates)
    }

    /**
     * Returns selected chapters
     * @return list of selected chapters
     */
    fun getSelectedChapters(): List<RecentChapter> {
        return adapter.selectedItems.map { adapter.getItem(it) as? RecentChapter }.filterNotNull()
    }

    /**
     * Called when item in list is clicked
     * @param position position of clicked item
     */
    override fun onListItemClick(position: Int): Boolean {
        // Get item from position
        val item = adapter.getItem(position)
        if (item is RecentChapter) {
            if (actionMode != null && adapter.mode == FlexibleAdapter.MODE_MULTI) {
                toggleSelection(position)
                return true
            } else {
                openChapter(item)
                return false
            }
        }
        return false
    }

    /**
     * Called when item in list is long clicked
     * @param position position of clicked item
     */
    override fun onListItemLongClick(position: Int) {
        if (actionMode == null)
            actionMode = activity.startSupportActionMode(this)

        toggleSelection(position)
    }

    /**
     * Called to toggle selection
     * @param position position of selected item
     */
    private fun toggleSelection(position: Int) {
        adapter.toggleSelection(position, false)

        val count = adapter.selectedItemCount
        if (count == 0) {
            actionMode?.finish()
        } else {
            setContextTitle(count)
            actionMode?.invalidate()
        }
    }

    /**
     * Set the context title
     * @param count count of selected items
     */
    private fun setContextTitle(count: Int) {
        actionMode?.title = getString(R.string.label_selected, count)
    }

    /**
     * Open chapter in reader
     * @param chapter selected chapter
     */
    private fun openChapter(chapter: RecentChapter) {
        val intent = ReaderActivity.newIntent(activity, chapter.manga, chapter)
        startActivity(intent)
    }

    /**
     * Download selected items
     * @param chapters list of selected [RecentChapter]s
     */
    fun downloadChapters(chapters: List<RecentChapter>) {
        destroyActionModeIfNeeded()
        presenter.downloadChapters(chapters)
    }

    /**
     * Populate adapter with chapters
     * @param chapters list of [Any]
     */
    fun onNextRecentChapters(chapters: List<Any>) {
        (activity as MainActivity).updateEmptyView(chapters.isEmpty(),
                R.string.information_no_recent, R.drawable.ic_update_black_128dp)

        destroyActionModeIfNeeded()
        adapter.setItems(chapters)
    }

    /**
     * Update download status of chapter
     * @param download [Download] object containing download progress.
     */
    fun onChapterStatusChange(download: Download) {
        getHolder(download)?.notifyStatus(download.status)
    }

    /**
     * Returns holder belonging to chapter
     * @param download [Download] object containing download progress.
     */
    private fun getHolder(download: Download): RecentChaptersHolder? {
        return recycler.findViewHolderForItemId(download.chapter.id!!) as? RecentChaptersHolder
    }

    /**
     * Mark chapter as read
     * @param chapters list of chapters
     */
    fun markAsRead(chapters: List<RecentChapter>) {
        presenter.markChapterRead(chapters, true)
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            deleteChapters(chapters)
        }
    }

    /**
     * Delete selected chapters
     * @param chapters list of [RecentChapter] objects
     */
    fun deleteChapters(chapters: List<RecentChapter>) {
        destroyActionModeIfNeeded()
        DeletingChaptersDialog().show(childFragmentManager, DeletingChaptersDialog.TAG)
        presenter.deleteChapters(chapters)
    }

    /**
     * Destory [ActionMode] if it's shown
     */
    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    /**
     * Mark chapter as unread
     * @param chapters list of selected [RecentChapter]
     */
    fun markAsUnread(chapters: List<RecentChapter>) {
        presenter.markChapterRead(chapters, false)
    }

    /**
     * Start downloading chapter
     * @param chapter selected chapter with manga
     */
    fun downloadChapter(chapter: RecentChapter) {
        presenter.downloadChapter(chapter)
    }

    /**
     * Start deleting chapter
     * @param chapter selected chapter with manga
     */
    fun deleteChapter(chapter: RecentChapter) {
        DeletingChaptersDialog().show(childFragmentManager, DeletingChaptersDialog.TAG)
        presenter.deleteChapters(listOf(chapter))
    }

    /**
     * Called when chapters are deleted
     */
    fun onChaptersDeleted() {
        dismissDeletingDialog()
        adapter.notifyDataSetChanged()
    }

    /**
     * Called when error while deleting
     * @param error error message
     */
    fun onChaptersDeletedError(error: Throwable) {
        dismissDeletingDialog()
        Timber.e(error)
    }

    /**
     * Called to dismiss deleting dialog
     */
    fun dismissDeletingDialog() {
        (childFragmentManager.findFragmentByTag(DeletingChaptersDialog.TAG) as? DialogFragment)?.dismiss()
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    /**
     * Called when ActionMode item clicked
     * @param mode the ActionMode object
     * @param item item from ActionMode.
     */
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_mark_as_read -> markAsRead(getSelectedChapters())
            R.id.action_mark_as_unread -> markAsUnread(getSelectedChapters())
            R.id.action_download -> downloadChapters(getSelectedChapters())
            R.id.action_delete -> {
                MaterialDialog.Builder(activity)
                        .content(R.string.confirm_delete_chapters)
                        .positiveText(android.R.string.yes)
                        .negativeText(android.R.string.no)
                        .onPositive { dialog, action -> deleteChapters(getSelectedChapters()) }
                        .show()
            }
            else -> return false
        }
        return true
    }

    /**
     * Called when ActionMode created.
     * @param mode the ActionMode object
     * @param menu menu object of ActionMode
     */
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.chapter_recent_selection, menu)
        adapter.mode = FlexibleAdapter.MODE_MULTI
        return true
    }

    /**
     * Called when ActionMode destroyed
     * @param mode the ActionMode object
     */
    override fun onDestroyActionMode(mode: ActionMode?) {
        adapter.mode = FlexibleAdapter.MODE_SINGLE
        adapter.clearSelection()
        actionMode = null
    }

}