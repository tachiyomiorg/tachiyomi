package eu.kanade.tachiyomi.ui.recent_updates

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.view.ActionMode
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.getResourceDrawable
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.DeletingChaptersDialog
import eu.kanade.tachiyomi.widget.DividerItemDecoration
import eu.kanade.tachiyomi.widget.NpaLinearLayoutManager
import kotlinx.android.synthetic.main.fragment_recent_chapters.*
import nucleus.factory.RequiresPresenter
import timber.log.Timber

/**
 * Fragment that shows recent chapters.
 * Uses [R.layout.fragment_recent_chapters].
 * UI related actions should be called from here.
 */
@RequiresPresenter(RecentChaptersPresenter::class)
class RecentChaptersFragment : BaseRxFragment<RecentChaptersPresenter>(), ActionMode.Callback, FlexibleViewHolder.OnListItemClickListener {
    companion object {
        /**
         * Create new RecentChaptersFragment.
         * @return a new instance of [RecentChaptersFragment].
         */
        @JvmStatic
        fun newInstance(): RecentChaptersFragment {
            return RecentChaptersFragment()
        }
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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        // Inflate view
        return inflater.inflate(R.layout.fragment_recent_chapters, container, false)
    }

    /**
     * Called when view is created
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
     * Returns selected chapters
     * @return list of [MangaChapter]s
     */
    fun getSelectedChapters(): List<MangaChapter> {
        return adapter.selectedItems.map { adapter.getItem(it) as? MangaChapter }.filterNotNull()
    }

    /**
     * Called when item in list is clicked
     * @param position position of clicked item
     */
    override fun onListItemClick(position: Int): Boolean {
        // Get item from position
        val item = adapter.getItem(position)
        if (item is MangaChapter) {
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
     * @param mangaChapter selected [MangaChapter]
     */
    private fun openChapter(mangaChapter: MangaChapter) {
        val intent = ReaderActivity.newIntent(activity, mangaChapter.manga, mangaChapter.chapter)
        startActivity(intent)
    }

    /**
     * Download selected items
     * @param mangaChapters list of selected [MangaChapter]s
     */
    fun downloadChapters(mangaChapters: List<MangaChapter>) {
        destroyActionModeIfNeeded()
        presenter.downloadChapters(mangaChapters)
    }

    /**
     * Populate adapter with chapters
     * @param chapters list of [Any]
     */
    fun onNextMangaChapters(chapters: List<Any>) {
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
        getHolder(download)?.onStatusChange(download.status)

    }

    /**
     * Returns holder belonging to chapter
     * @param download [Download] object containing download progress.
     */
    private fun getHolder(download: Download): RecentChaptersHolder? {
        return recycler.findViewHolderForItemId(download.chapter.id) as? RecentChaptersHolder
    }

    /**
     * Mark chapter as read
     * @param mangaChapters list of [MangaChapter] objects
     */
    fun markAsRead(mangaChapters: List<MangaChapter>) {
        presenter.markChapterRead(mangaChapters, true)
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            deleteChapters(mangaChapters)
        }
    }

    /**
     * Delete selected chapters
     * @param mangaChapters list of [MangaChapter] objects
     */
    fun deleteChapters(mangaChapters: List<MangaChapter>) {
        destroyActionModeIfNeeded()
        DeletingChaptersDialog().show(childFragmentManager, DeletingChaptersDialog.TAG)
        presenter.deleteChapters(mangaChapters)
    }

    /**
     * Destory [ActionMode] if it's shown
     */
    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    /**
     * Mark chapter as unread
     * @param mangaChapters list of selected [MangaChapter]
     */
    fun markAsUnread(mangaChapters: List<MangaChapter>) {
        presenter.markChapterRead(mangaChapters, false)
    }

    /**
     * Start downloading chapter
     * @param item selected chapter with manga
     */
    fun downloadChapter(item: MangaChapter) {
        presenter.downloadChapter(item)
    }

    /**
     * Start deleting chapter
     * @param item selected chapter with manga
     */
    fun deleteChapter(item: MangaChapter) {
        DeletingChaptersDialog().show(childFragmentManager, DeletingChaptersDialog.TAG)
        presenter.deleteChapter(item)
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
        Timber.e(error, error.message)
    }

    /**
     * Called to dismiss deleting dialog
     */
    fun dismissDeletingDialog() {
        (childFragmentManager.findFragmentByTag(DeletingChaptersDialog.TAG) as? DialogFragment)?.dismiss()
    }

}