package eu.kanade.tachiyomi.ui.recent_updates

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.*
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import com.jakewharton.rxbinding.support.v7.widget.scrollStateChanges
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.ui.base.controller.NoToolbarElevationController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.popControllerWithTag
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.recent_chapters_controller.*
import timber.log.Timber

/**
 * Fragment that shows recent chapters.
 * Uses [R.layout.recent_chapters_controller].
 * UI related actions should be called from here.
 */
class RecentChaptersController : NucleusController<RecentChaptersPresenter>(),
        NoToolbarElevationController,
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        FlexibleAdapter.OnUpdateListener,
        ConfirmDeleteChaptersDialog.Listener,
        RecentChaptersAdapter.OnCoverClickListener {

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    /**
     * Adapter containing the recent chapters.
     */
    var adapter: RecentChaptersAdapter? = null
        private set

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_recent_updates)
    }

    override fun createPresenter(): RecentChaptersPresenter {
        return RecentChaptersPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.recent_chapters_controller, container, false)
    }

    /**
     * Called when view is created
     * @param view created view
     */
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Init RecyclerView and adapter
        val layoutManager = LinearLayoutManager(view.context)
        recycler.layoutManager = layoutManager
        recycler.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recycler.setHasFixedSize(true)
        adapter = RecentChaptersAdapter(this@RecentChaptersController)
        recycler.adapter = adapter

        recycler.scrollStateChanges().subscribeUntilDestroy {
            // Disable swipe refresh when view is not at the top
            val firstPos = layoutManager.findFirstCompletelyVisibleItemPosition()
            swipe_refresh.isEnabled = firstPos <= 0
        }

        swipe_refresh.setDistanceToTriggerSync((2 * 64 * view.resources.displayMetrics.density).toInt())
        swipe_refresh.refreshes().subscribeUntilDestroy {
            if (!LibraryUpdateService.isRunning(view.context)) {
                LibraryUpdateService.start(view.context)
                view.context.toast(R.string.action_update_library)
            }
            // It can be a very long operation, so we disable swipe refresh and show a toast.
            swipe_refresh.isRefreshing = false
        }
    }

    override fun onDestroyView(view: View) {
        adapter = null
        actionMode = null
        super.onDestroyView(view)
    }

    /**
     * Returns selected chapters
     * @return list of selected chapters
     */
    fun getSelectedChapters(): List<RecentChapterItem> {
        val adapter = adapter ?: return emptyList()
        return adapter.selectedPositions.mapNotNull { adapter.getItem(it) as? RecentChapterItem }
    }

    /**
     * Called when item in list is clicked
     * @param position position of clicked item
     */
    override fun onItemClick(view: View, position: Int): Boolean {
        val adapter = adapter ?: return false

        // Get item from position
        val item = adapter.getItem(position) as? RecentChapterItem ?: return false
        if (actionMode != null && adapter.mode == SelectableAdapter.Mode.MULTI) {
            toggleSelection(position)
            return true
        } else {
            openChapter(item)
            return false
        }
    }

    /**
     * Called when item in list is long clicked
     * @param position position of clicked item
     */
    override fun onItemLongClick(position: Int) {
        if (actionMode == null)
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)

        toggleSelection(position)
    }

    /**
     * Called to toggle selection
     * @param position position of selected item
     */
    private fun toggleSelection(position: Int) {
        val adapter = adapter ?: return
        adapter.toggleSelection(position)
        actionMode?.invalidate()
    }

    /**
     * Open chapter in reader
     * @param chapter selected chapter
     */
    private fun openChapter(item: RecentChapterItem) {
        val activity = activity ?: return
        val intent = ReaderActivity.newIntent(activity, item.manga, item.chapter)
        startActivity(intent)
    }

    /**
     * Download selected items
     * @param chapters list of selected [RecentChapter]s
     */
    fun downloadChapters(chapters: List<RecentChapterItem>) {
        destroyActionModeIfNeeded()
        presenter.downloadChapters(chapters)
    }

    /**
     * Populate adapter with chapters
     * @param chapters list of [Any]
     */
    fun onNextRecentChapters(chapters: List<IFlexible<*>>) {
        destroyActionModeIfNeeded()
        adapter?.updateDataSet(chapters)
    }

    override fun onUpdateEmptyView(size: Int) {
        if (size > 0) {
            empty_view?.hide()
        } else {
            empty_view?.show(R.drawable.ic_update_black_128dp, R.string.information_no_recent)
        }
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
    private fun getHolder(download: Download): RecentChapterHolder? {
        return recycler?.findViewHolderForItemId(download.chapter.id!!) as? RecentChapterHolder
    }

    /**
     * Mark chapter as read
     * @param chapters list of chapters
     */
    fun markAsRead(chapters: List<RecentChapterItem>) {
        presenter.markChapterRead(chapters, true)
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            deleteChapters(chapters)
        }
    }

    override fun deleteChapters(chaptersToDelete: List<RecentChapterItem>) {
        destroyActionModeIfNeeded()
        DeletingChaptersDialog().showDialog(router)
        presenter.deleteChapters(chaptersToDelete)
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
    fun markAsUnread(chapters: List<RecentChapterItem>) {
        presenter.markChapterRead(chapters, false)
    }

    /**
     * Start downloading chapter
     * @param chapter selected chapter with manga
     */
    fun downloadChapter(chapter: RecentChapterItem) {
        presenter.downloadChapters(listOf(chapter))
    }

    /**
     * Start deleting chapter
     * @param chapter selected chapter with manga
     */
    fun deleteChapter(chapter: RecentChapterItem) {
        DeletingChaptersDialog().showDialog(router)
        presenter.deleteChapters(listOf(chapter))
    }

    override fun onCoverClick(position: Int) {
        val chapterClicked = adapter?.getItem(position) as? RecentChapterItem ?: return
        openManga(chapterClicked)

    }

    fun openManga(chapter: RecentChapterItem) {
        router.pushController(MangaController(chapter.manga).withFadeTransaction())
    }

    /**
     * Called when chapters are deleted
     */
    fun onChaptersDeleted() {
        dismissDeletingDialog()
        adapter?.notifyDataSetChanged()
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
        router.popControllerWithTag(DeletingChaptersDialog.TAG)
    }

    /**
     * Called when ActionMode created.
     * @param mode the ActionMode object
     * @param menu menu object of ActionMode
     */
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.chapter_recent_selection, menu)
        adapter?.mode = SelectableAdapter.Mode.MULTI
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter?.selectedItemCount ?: 0
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = resources?.getString(R.string.label_selected, count)
        }
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
            R.id.action_delete -> ConfirmDeleteChaptersDialog(this, getSelectedChapters())
                    .showDialog(router)
            else -> return false
        }
        return true
    }

    /**
     * Called when ActionMode destroyed
     * @param mode the ActionMode object
     */
    override fun onDestroyActionMode(mode: ActionMode?) {
        adapter?.mode = SelectableAdapter.Mode.IDLE
        adapter?.clearSelection()
        actionMode = null
    }

}
