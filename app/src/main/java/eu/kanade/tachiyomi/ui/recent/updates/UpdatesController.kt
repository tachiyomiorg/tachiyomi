package eu.kanade.tachiyomi.ui.recent.updates

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import com.jakewharton.rxbinding.support.v7.widget.scrollStateChanges
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.base.controller.NoToolbarElevationController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.popControllerWithTag
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.android.synthetic.main.updates_controller.action_toolbar
import kotlinx.android.synthetic.main.updates_controller.empty_view
import kotlinx.android.synthetic.main.updates_controller.recycler
import kotlinx.android.synthetic.main.updates_controller.swipe_refresh
import timber.log.Timber

/**
 * Fragment that shows recent chapters.
 * Uses [R.layout.updates_controller].
 * UI related actions should be called from here.
 */
class UpdatesController : NucleusController<UpdatesPresenter>(),
        RootController,
        NoToolbarElevationController,
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        FlexibleAdapter.OnUpdateListener,
        ConfirmDeleteChaptersDialog.Listener,
        UpdatesAdapter.OnCoverClickListener {

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    /**
     * Adapter containing the recent chapters.
     */
    var adapter: UpdatesAdapter? = null
        private set

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_recent_updates)
    }

    override fun createPresenter(): UpdatesPresenter {
        return UpdatesPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.updates_controller, container, false)
    }

    /**
     * Called when view is created
     * @param view created view
     */
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        view.context.notificationManager.cancel(Notifications.ID_NEW_CHAPTERS)
        // Init RecyclerView and adapter
        val layoutManager = LinearLayoutManager(view.context)
        recycler.layoutManager = layoutManager
        recycler.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recycler.setHasFixedSize(true)
        adapter = UpdatesAdapter(this@UpdatesController)
        recycler.adapter = adapter

        recycler.scrollStateChanges().subscribeUntilDestroy {
            // Disable swipe refresh when view is not at the top
            val firstPos = layoutManager.findFirstCompletelyVisibleItemPosition()
            swipe_refresh.isEnabled = firstPos <= 0
        }

        swipe_refresh.setDistanceToTriggerSync((2 * 64 * view.resources.displayMetrics.density).toInt())
        swipe_refresh.refreshes().subscribeUntilDestroy {
            updateLibrary()

            // It can be a very long operation, so we disable swipe refresh and show a toast.
            swipe_refresh.isRefreshing = false
        }
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        action_toolbar.destroy()
        adapter = null
        super.onDestroyView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.updates, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_update_library -> updateLibrary()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun updateLibrary() {
        activity?.let {
            if (LibraryUpdateService.start(it)) {
                it.toast(R.string.updating_library)
            }
        }
    }

    /**
     * Returns selected chapters
     * @return list of selected chapters
     */
    private fun getSelectedChapters(): List<UpdatesItem> {
        val adapter = adapter ?: return emptyList()
        return adapter.selectedPositions.mapNotNull { adapter.getItem(it) as? UpdatesItem }
    }

    /**
     * Called when item in list is clicked
     * @param position position of clicked item
     */
    override fun onItemClick(view: View, position: Int): Boolean {
        val adapter = adapter ?: return false

        // Get item from position
        val item = adapter.getItem(position) as? UpdatesItem ?: return false
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
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)
            action_toolbar.show(
                    actionMode!!,
                    R.menu.updates_chapter_selection
            ) { onActionItemClicked(actionMode!!, it!!) }
        }

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
    private fun openChapter(item: UpdatesItem) {
        val activity = activity ?: return
        val intent = ReaderActivity.newIntent(activity, item.manga, item.chapter)
        startActivity(intent)
    }

    /**
     * Download selected items
     * @param chapters list of selected [UpdatesItem]s
     */
    private fun downloadChapters(chapters: List<UpdatesItem>) {
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
            empty_view?.show(R.string.information_no_recent)
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
    private fun getHolder(download: Download): UpdatesHolder? {
        return recycler?.findViewHolderForItemId(download.chapter.id!!) as? UpdatesHolder
    }

    /**
     * Mark chapter as read
     * @param chapters list of chapters
     */
    private fun markAsRead(chapters: List<UpdatesItem>) {
        presenter.markChapterRead(chapters, true)
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            deleteChapters(chapters)
        }
    }

    /**
     * Mark chapter as unread
     * @param chapters list of selected [UpdatesItem]
     */
    private fun markAsUnread(chapters: List<UpdatesItem>) {
        presenter.markChapterRead(chapters, false)
    }

    override fun deleteChapters(chaptersToDelete: List<UpdatesItem>) {
        DeletingChaptersDialog().showDialog(router)
        presenter.deleteChapters(chaptersToDelete)
    }

    /**
     * Destory [ActionMode] if it's shown
     */
    private fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    override fun onCoverClick(position: Int) {
        val chapterClicked = adapter?.getItem(position) as? UpdatesItem ?: return
        openManga(chapterClicked)
    }

    private fun openManga(chapter: UpdatesItem) {
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
    private fun dismissDeletingDialog() {
        router.popControllerWithTag(DeletingChaptersDialog.TAG)
    }

    /**
     * Called when ActionMode created.
     * @param mode the ActionMode object
     * @param menu menu object of ActionMode
     */
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.generic_selection, menu)
        adapter?.mode = SelectableAdapter.Mode.MULTI
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter?.selectedItemCount ?: 0
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = count.toString()

            val chapters = getSelectedChapters()
            action_toolbar.findItem(R.id.action_download)?.isVisible = chapters.any { !it.isDownloaded }
            action_toolbar.findItem(R.id.action_delete)?.isVisible = chapters.any { it.isDownloaded }
            action_toolbar.findItem(R.id.action_mark_as_read)?.isVisible = chapters.any { !it.chapter.read }
            action_toolbar.findItem(R.id.action_mark_as_unread)?.isVisible = chapters.all { it.chapter.read }
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
            R.id.action_select_all -> selectAll()
            R.id.action_select_inverse -> selectInverse()
            R.id.action_download -> downloadChapters(getSelectedChapters())
            R.id.action_delete -> ConfirmDeleteChaptersDialog(this, getSelectedChapters())
                    .showDialog(router)
            R.id.action_mark_as_read -> markAsRead(getSelectedChapters())
            R.id.action_mark_as_unread -> markAsUnread(getSelectedChapters())
            else -> return false
        }
        return true
    }

    /**
     * Called when ActionMode destroyed
     * @param mode the ActionMode object
     */
    override fun onDestroyActionMode(mode: ActionMode?) {
        action_toolbar.hide()
        adapter?.mode = SelectableAdapter.Mode.IDLE
        adapter?.clearSelection()
        actionMode = null
    }

    private fun selectAll() {
        val adapter = adapter ?: return
        adapter.selectAll()
        actionMode?.invalidate()
    }

    private fun selectInverse() {
        val adapter = adapter ?: return
        for (i in 0..adapter.itemCount) {
            adapter.toggleSelection(i)
        }
        actionMode?.invalidate()
        adapter.notifyDataSetChanged()
    }
}
