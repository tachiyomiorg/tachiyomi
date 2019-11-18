package eu.kanade.tachiyomi.ui.manga.chapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import com.jakewharton.rxbinding.view.clicks
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.popControllerWithTag
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.getCoordinates
import eu.kanade.tachiyomi.util.snack
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.chapters_controller.*
import timber.log.Timber

class ChaptersController : NucleusController<ChaptersPresenter>(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        ChaptersAdapter.OnMenuItemClickListener,
        SetDisplayModeDialog.Listener,
        SetSortingDialog.Listener,
        DownloadChaptersDialog.Listener,
        DownloadCustomChaptersDialog.Listener,
        DeleteChaptersDialog.Listener {

    /**
     * Adapter containing a list of chapters.
     */
    private var adapter: ChaptersAdapter? = null

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    /**
     * Selected items. Used to restore selections after a rotation.
     */
    private val selectedItems = mutableSetOf<ChapterItem>()

    init {
        setHasOptionsMenu(true)
        setOptionsMenuHidden(true)
    }

    override fun createPresenter(): ChaptersPresenter {
        val ctrl = parentController as MangaController
        return ChaptersPresenter(ctrl.manga!!, ctrl.source!!,
                ctrl.chapterCountRelay, ctrl.lastUpdateRelay, ctrl.mangaFavoriteRelay)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.chapters_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Init RecyclerView and adapter
        adapter = ChaptersAdapter(this, view.context)

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(view.context)
        recycler.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recycler.setHasFixedSize(true)
        adapter?.fastScroller = fast_scroller

        swipe_refresh.refreshes().subscribeUntilDestroy { fetchChaptersFromSource() }

        fab.clicks().subscribeUntilDestroy {
            val item = presenter.getNextUnreadChapter()
            if (item != null) {
                // Create animation listener
                val revealAnimationListener: Animator.AnimatorListener = object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        openChapter(item.chapter, true)
                    }
                }

                // Get coordinates and start animation
                val coordinates = fab.getCoordinates()
                if (!reveal_view.showRevealEffect(coordinates.x, coordinates.y, revealAnimationListener)) {
                    openChapter(item.chapter)
                }
            } else {
                view.context.toast(R.string.no_next_chapter)
            }
        }
    }

    override fun onDestroyView(view: View) {
        adapter = null
        actionMode = null
        super.onDestroyView(view)
    }

    override fun onActivityResumed(activity: Activity) {
        if (view == null) return

        // Check if animation view is visible
        if (reveal_view.visibility == View.VISIBLE) {
            // Show the unReveal effect
            val coordinates = fab.getCoordinates()
            reveal_view.hideRevealEffect(coordinates.x, coordinates.y, 1920)
        }
        super.onActivityResumed(activity)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.chapters, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        // Initialize menu items.
        val menuFilterRead = menu.findItem(R.id.action_filter_read) ?: return
        val menuFilterUnread = menu.findItem(R.id.action_filter_unread)
        val menuFilterDownloaded = menu.findItem(R.id.action_filter_downloaded)
        val menuFilterBookmarked = menu.findItem(R.id.action_filter_bookmarked)

        // Set correct checkbox values.
        menuFilterRead.isChecked = presenter.onlyRead()
        menuFilterUnread.isChecked = presenter.onlyUnread()
        menuFilterDownloaded.isChecked = presenter.onlyDownloaded()
        menuFilterBookmarked.isChecked = presenter.onlyBookmarked()

        if (presenter.onlyRead())
            //Disable unread filter option if read filter is enabled.
            menuFilterUnread.isEnabled = false
        if (presenter.onlyUnread())
            //Disable read filter option if unread filter is enabled.
            menuFilterRead.isEnabled = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_display_mode -> showDisplayModeDialog()
            R.id.manga_download -> showDownloadDialog()
            R.id.action_sorting_mode -> showSortingDialog()
            R.id.action_filter_unread -> {
                item.isChecked = !item.isChecked
                presenter.setUnreadFilter(item.isChecked)
                activity?.invalidateOptionsMenu()
            }
            R.id.action_filter_read -> {
                item.isChecked = !item.isChecked
                presenter.setReadFilter(item.isChecked)
                activity?.invalidateOptionsMenu()
            }
            R.id.action_filter_downloaded -> {
                item.isChecked = !item.isChecked
                presenter.setDownloadedFilter(item.isChecked)
            }
            R.id.action_filter_bookmarked -> {
                item.isChecked = !item.isChecked
                presenter.setBookmarkedFilter(item.isChecked)
            }
            R.id.action_filter_empty -> {
                presenter.removeFilters()
                activity?.invalidateOptionsMenu()
            }
            R.id.action_sort -> presenter.revertSortOrder()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun onNextChapters(chapters: List<ChapterItem>) {
        // If the list is empty, fetch chapters from source if the conditions are met
        // We use presenter chapters instead because they are always unfiltered
        if (presenter.chapters.isEmpty())
            initialFetchChapters()

        val adapter = adapter ?: return
        adapter.updateDataSet(chapters)

        if (selectedItems.isNotEmpty()) {
            adapter.clearSelection() // we need to start from a clean state, index may have changed
            createActionModeIfNeeded()
            selectedItems.forEach { item ->
                val position = adapter.indexOf(item)
                if (position != -1 && !adapter.isSelected(position)) {
                    adapter.toggleSelection(position)
                }
            }
            actionMode?.invalidate()
        }

    }

    private fun initialFetchChapters() {
        // Only fetch if this view is from the catalog and it hasn't requested previously
        if ((parentController as MangaController).fromCatalogue && !presenter.hasRequested) {
            fetchChaptersFromSource()
        }
    }

    private fun fetchChaptersFromSource() {
        swipe_refresh?.isRefreshing = true
        presenter.fetchChaptersFromSource()
    }

    fun onFetchChaptersDone() {
        swipe_refresh?.isRefreshing = false
    }

    fun onFetchChaptersError(error: Throwable) {
        swipe_refresh?.isRefreshing = false
        activity?.toast(error.message)
    }

    fun onChapterStatusChange(download: Download) {
        getHolder(download.chapter)?.notifyStatus(download.status)
    }

    private fun getHolder(chapter: Chapter): ChapterHolder? {
        return recycler?.findViewHolderForItemId(chapter.id!!) as? ChapterHolder
    }

    fun openChapter(chapter: Chapter, hasAnimation: Boolean = false) {
        val activity = activity ?: return
        val intent = ReaderActivity.newIntent(activity, presenter.manga, chapter)
        if (hasAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    override fun onItemClick(position: Int): Boolean {
        val adapter = adapter ?: return false
        val item = adapter.getItem(position) ?: return false
        if (actionMode != null && adapter.mode == SelectableAdapter.Mode.MULTI) {
            toggleSelection(position)
            return true
        } else {
            openChapter(item.chapter)
            return false
        }
    }

    override fun onItemLongClick(position: Int) {
        createActionModeIfNeeded()
        toggleSelection(position)
    }

    // SELECTIONS & ACTION MODE

    private fun toggleSelection(position: Int) {
        val adapter = adapter ?: return
        val item = adapter.getItem(position) ?: return
        adapter.toggleSelection(position)
        if (adapter.isSelected(position)) {
            selectedItems.add(item)
        } else {
            selectedItems.remove(item)
        }
        // Only show action_select_between when there are two chapters selected.
        actionMode?.menu?.findItem(R.id.action_select_between)?.isVisible = (selectedItems.size == 2)
        actionMode?.invalidate()
    }

    private fun getSelectedChapters(): List<ChapterItem> {
        val adapter = adapter ?: return emptyList()
        return adapter.selectedPositions.mapNotNull { adapter.getItem(it) }
    }

    private fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
        }
    }

    private fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.chapter_selection, menu)
        adapter?.mode = SelectableAdapter.Mode.MULTI
        return true
    }

    @SuppressLint("StringFormatInvalid")
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

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> selectAll()
            R.id.action_mark_as_read -> markAsRead(getSelectedChapters())
            R.id.action_mark_as_unread -> markAsUnread(getSelectedChapters())
            R.id.action_download -> downloadChapters(getSelectedChapters())
            R.id.action_delete -> showDeleteChaptersConfirmationDialog()
            R.id.action_select_between -> selectBetween()
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        adapter?.mode = SelectableAdapter.Mode.SINGLE
        adapter?.clearSelection()
        selectedItems.clear()
        actionMode = null
    }

    override fun onMenuItemClick(position: Int, item: MenuItem) {
        val chapter = adapter?.getItem(position) ?: return
        val chapters = listOf(chapter)

        when (item.itemId) {
            R.id.action_download -> downloadChapters(chapters)
            R.id.action_bookmark -> bookmarkChapters(chapters, true)
            R.id.action_remove_bookmark -> bookmarkChapters(chapters, false)
            R.id.action_delete -> deleteChapters(chapters)
            R.id.action_mark_as_read -> markAsRead(chapters)
            R.id.action_mark_as_unread -> markAsUnread(chapters)
            R.id.action_mark_previous_as_read -> markPreviousAsRead(chapter)
        }
    }

    // SELECTION MODE ACTIONS

    private fun selectAll() {
        val adapter = adapter ?: return
        adapter.selectAll()
        selectedItems.addAll(adapter.items)
        actionMode?.invalidate()
    }

    private fun selectBetween() {
        val adapter = adapter ?: return
        if (selectedItems.size == 2) {
            var first = adapter.items.indexOf(selectedItems.first())
            var last = adapter.items.indexOf(selectedItems.last())
            if (first > last) {
                first = last.also { last = first }
            }
            selectedItems.clear()
            selectedItems.addAll(adapter.items.subList(first, last + 1))
            selectedItems.forEach { adapter.addSelection(adapter.items.indexOf(it)) }
            adapter.notifyDataSetChanged()
            actionMode?.invalidate()
        }
    }

    private fun markAsRead(chapters: List<ChapterItem>) {
        presenter.markChaptersRead(chapters, true)
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            deleteChapters(chapters)
        }
    }

    private fun markAsUnread(chapters: List<ChapterItem>) {
        presenter.markChaptersRead(chapters, false)
    }

    private fun downloadChapters(chapters: List<ChapterItem>) {
        val view = view
        destroyActionModeIfNeeded()
        presenter.downloadChapters(chapters)
        if (view != null && !presenter.manga.favorite) {
            recycler?.snack(view.context.getString(R.string.snack_add_to_library), Snackbar.LENGTH_INDEFINITE) {
                setAction(R.string.action_add) {
                    presenter.addToLibrary()
                }
            }
        }
    }


    private fun showDeleteChaptersConfirmationDialog() {
        DeleteChaptersDialog(this).showDialog(router)
    }

    override fun deleteChapters() {
        deleteChapters(getSelectedChapters())
    }

    private fun markPreviousAsRead(chapter: ChapterItem) {
        val adapter = adapter ?: return
        val chapters = if (presenter.sortDescending()) adapter.items.reversed() else adapter.items
        val chapterPos = chapters.indexOf(chapter)
        if (chapterPos != -1) {
            markAsRead(chapters.take(chapterPos))
        }
    }

    private fun bookmarkChapters(chapters: List<ChapterItem>, bookmarked: Boolean) {
        destroyActionModeIfNeeded()
        presenter.bookmarkChapters(chapters, bookmarked)
    }

    fun deleteChapters(chapters: List<ChapterItem>) {
        destroyActionModeIfNeeded()
        if (chapters.isEmpty()) return

        DeletingChaptersDialog().showDialog(router)
        presenter.deleteChapters(chapters)
    }

    fun onChaptersDeleted() {
        dismissDeletingDialog()
        adapter?.notifyDataSetChanged()
    }

    fun onChaptersDeletedError(error: Throwable) {
        dismissDeletingDialog()
        Timber.e(error)
    }

    private fun dismissDeletingDialog() {
        router.popControllerWithTag(DeletingChaptersDialog.TAG)
    }

    // OVERFLOW MENU DIALOGS

    private fun showDisplayModeDialog() {
        val preselected = if (presenter.manga.displayMode == Manga.DISPLAY_NAME) 0 else 1
        SetDisplayModeDialog(this, preselected).showDialog(router)
    }

    override fun setDisplayMode(id: Int) {
        presenter.setDisplayMode(id)
        adapter?.notifyDataSetChanged()
    }

    private fun showSortingDialog() {
        val preselected = if (presenter.manga.sorting == Manga.SORTING_SOURCE) 0 else 1
        SetSortingDialog(this, preselected).showDialog(router)
    }

    override fun setSorting(id: Int) {
        presenter.setSorting(id)
    }

    private fun showDownloadDialog() {
        DownloadChaptersDialog(this).showDialog(router)
    }

    private fun getUnreadChaptersSorted() = presenter.chapters
            .filter { !it.read && it.status == Download.NOT_DOWNLOADED }
            .distinctBy { it.name }
            .sortedByDescending { it.source_order }

    override fun downloadCustomChapters(amount: Int) {
        val chaptersToDownload = getUnreadChaptersSorted().take(amount)
        if (chaptersToDownload.isNotEmpty()) {
            downloadChapters(chaptersToDownload)
        }
    }

    private fun showCustomDownloadDialog() {
        DownloadCustomChaptersDialog(this, presenter.chapters.size).showDialog(router)
    }


    override fun downloadChapters(choice: Int) {
        // i = 0: Download 1
        // i = 1: Download 5
        // i = 2: Download 10
        // i = 3: Download x
        // i = 4: Download unread
        // i = 5: Download all
        val chaptersToDownload = when (choice) {
            0 -> getUnreadChaptersSorted().take(1)
            1 -> getUnreadChaptersSorted().take(5)
            2 -> getUnreadChaptersSorted().take(10)
            3 -> {
                showCustomDownloadDialog()
                return
            }
            4 -> presenter.chapters.filter { !it.read }
            5 -> presenter.chapters
            else -> emptyList()
        }
        if (chaptersToDownload.isNotEmpty()) {
            downloadChapters(chaptersToDownload)
        }
    }
}
