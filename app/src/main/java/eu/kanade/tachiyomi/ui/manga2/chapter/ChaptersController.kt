package eu.kanade.tachiyomi.ui.manga2.chapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import com.jakewharton.rxbinding.view.clicks
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.manga2.MangaController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.getCoordinates
import eu.kanade.tachiyomi.util.snack
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.fragment_manga_chapters.view.*
import timber.log.Timber

class ChaptersController : NucleusController<ChaptersPresenter>(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    /**
     * Adapter containing a list of chapters.
     */
    private var adapter: ChaptersAdapter? = null

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    init {
        setHasOptionsMenu(true)
        setOptionsMenuHidden(true)
    }

    override fun createPresenter(): ChaptersPresenter {
        val mangaController = (parentController as MangaController)
        return ChaptersPresenter(mangaController.manga!!, mangaController.source!!)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.fragment_manga_chapters, container, false)
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        // Init RecyclerView and adapter
        adapter = ChaptersAdapter(this)

        with(view) {
            recycler.adapter = adapter
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            recycler.setHasFixedSize(true)
            // TODO enable in a future commit
//             adapter.setFastScroller(fast_scroller, context.getResourceColor(R.attr.colorAccent))
//             adapter.toggleFastScroller()

            swipe_refresh.refreshes().subscribeUntilDestroy { fetchChapters() }

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
                    context.toast(R.string.no_next_chapter)
                }
            }
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
        actionMode = null
    }

//    override fun onResume() {
//        // Check if animation view is visible
//        if (reveal_view.visibility == View.VISIBLE) {
//            // Show the unReveal effect
//            val coordinates = fab.getCoordinates()
//            reveal_view.hideRevealEffect(coordinates.x, coordinates.y, 1920)
//        }
//        super.onResume()
//    }

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

        destroyActionModeIfNeeded()
        adapter?.updateDataSet(chapters)
    }

    private fun initialFetchChapters() {
        // Only fetch if this view is from the catalog and it hasn't requested previously
        if (isCatalogueManga && !presenter.hasRequested) {
            fetchChapters()
        }
    }

    fun fetchChapters() {
        view?.swipe_refresh?.isRefreshing = true
        presenter.fetchChaptersFromSource()
    }

    fun onFetchChaptersDone() {
        view?.swipe_refresh?.isRefreshing = false
    }

    fun onFetchChaptersError(error: Throwable) {
        view?.swipe_refresh?.isRefreshing = false
        activity?.toast(error.message)
    }

    val isCatalogueManga: Boolean
        get() = (parentController as MangaController).fromCatalogue

    fun openChapter(chapter: Chapter, hasAnimation: Boolean = false) {
        val activity = activity ?: return
        val intent = ReaderActivity.newIntent(activity, presenter.manga, chapter)
        if (hasAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    private fun showDisplayModeDialog() {
        val activity = activity ?: return
        val adapter = adapter ?: return

        // Get available modes, ids and the selected mode
        val modes = intArrayOf(R.string.show_title, R.string.show_chapter_number)
        val ids = intArrayOf(Manga.DISPLAY_NAME, Manga.DISPLAY_NUMBER)
        val selectedIndex = if (presenter.manga.displayMode == Manga.DISPLAY_NAME) 0 else 1

        MaterialDialog.Builder(activity)
                .title(R.string.action_display_mode)
                .items(modes.map { activity.getString(it) })
                .itemsIds(ids)
                .itemsCallbackSingleChoice(selectedIndex) { _, itemView, _, _ ->
                    // Save the new display mode
                    presenter.setDisplayMode(itemView.id)
                    // Refresh ui
                    adapter.notifyItemRangeChanged(0, adapter.itemCount)
                    true
                }
                .show()
    }

    private fun showSortingDialog() {
        val activity = activity ?: return

        // Get available modes, ids and the selected mode
        val modes = intArrayOf(R.string.sort_by_source, R.string.sort_by_number)
        val ids = intArrayOf(Manga.SORTING_SOURCE, Manga.SORTING_NUMBER)
        val selectedIndex = if (presenter.manga.sorting == Manga.SORTING_SOURCE) 0 else 1

        MaterialDialog.Builder(activity)
                .title(R.string.sorting_mode)
                .items(modes.map { activity.getString(it) })
                .itemsIds(ids)
                .itemsCallbackSingleChoice(selectedIndex) { _, itemView, _, _ ->
                    // Save the new sorting mode
                    presenter.setSorting(itemView.id)
                    true
                }
                .show()
    }

    private fun showDownloadDialog() {
        val activity = activity ?: return

        // Get available modes
        val modes = intArrayOf(R.string.download_1, R.string.download_5, R.string.download_10,
                R.string.download_unread, R.string.download_all)

        MaterialDialog.Builder(activity)
                .title(R.string.manga_download)
                .negativeText(android.R.string.cancel)
                .items(modes.map { activity.getString(it) })
                .itemsCallback { _, _, i, _ ->

                    fun getUnreadChaptersSorted() = presenter.chapters
                            .filter { !it.read && it.status == Download.NOT_DOWNLOADED }
                            .distinctBy { it.name }
                            .sortedByDescending { it.source_order }

                    // i = 0: Download 1
                    // i = 1: Download 5
                    // i = 2: Download 10
                    // i = 3: Download unread
                    // i = 4: Download all
                    val chaptersToDownload = when (i) {
                        0 -> getUnreadChaptersSorted().take(1)
                        1 -> getUnreadChaptersSorted().take(5)
                        2 -> getUnreadChaptersSorted().take(10)
                        3 -> presenter.chapters.filter { !it.read }
                        4 -> presenter.chapters
                        else -> emptyList()
                    }

                    if (chaptersToDownload.isNotEmpty()) {
                        downloadChapters(chaptersToDownload)
                    }
                }
                .show()
    }

    fun onChapterStatusChange(download: Download) {
        getHolder(download.chapter)?.notifyStatus(download.status)
    }

    private fun getHolder(chapter: Chapter): ChapterHolder? {
        return view?.recycler?.findViewHolderForItemId(chapter.id!!) as? ChapterHolder
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.chapter_selection, menu)
        adapter?.mode = FlexibleAdapter.MODE_MULTI
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter?.selectedItemCount ?: 0
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = resources?.getString(R.string.label_selected, count)
            menu.findItem(R.id.action_edit_cover)?.isVisible = count == 1
        }
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> selectAll()
            R.id.action_mark_as_read -> markAsRead(getSelectedChapters())
            R.id.action_mark_as_unread -> markAsUnread(getSelectedChapters())
            R.id.action_download -> downloadChapters(getSelectedChapters())
            R.id.action_delete -> {
                val activity = activity ?: return false
                MaterialDialog.Builder(activity)
                        .content(R.string.confirm_delete_chapters)
                        .positiveText(android.R.string.yes)
                        .negativeText(android.R.string.no)
                        .onPositive { _, _ -> deleteChapters(getSelectedChapters()) }
                        .show()
            }
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        adapter?.mode = FlexibleAdapter.MODE_SINGLE
        adapter?.clearSelection()
        actionMode = null
    }

    fun getSelectedChapters(): List<ChapterItem> {
        val adapter = adapter ?: return emptyList()
        return adapter.selectedPositions.map { adapter.getItem(it) }
    }

    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    fun selectAll() {
        val adapter = adapter ?: return
        adapter.selectAll()
        actionMode?.invalidate()
    }

    fun markAsRead(chapters: List<ChapterItem>) {
        presenter.markChaptersRead(chapters, true)
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            deleteChapters(chapters)
        }
    }

    fun markAsUnread(chapters: List<ChapterItem>) {
        presenter.markChaptersRead(chapters, false)
    }

    fun markPreviousAsRead(chapter: ChapterItem) {
        val adapter = adapter ?: return
        val chapters = if (presenter.sortDescending()) adapter.items.reversed() else adapter.items
        val chapterPos = chapters.indexOf(chapter)
        if (chapterPos != -1) {
            presenter.markChaptersRead(chapters.take(chapterPos), true)
        }
    }

    fun downloadChapters(chapters: List<ChapterItem>) {
        val view = view
        destroyActionModeIfNeeded()
        presenter.downloadChapters(chapters)
        if (view != null && !presenter.manga.favorite) {
            view.recycler?.snack(view.context.getString(R.string.snack_add_to_library), Snackbar.LENGTH_INDEFINITE) {
                setAction(R.string.action_add) {
                    presenter.addToLibrary()
                }
            }
        }
    }

    fun bookmarkChapters(chapters: List<ChapterItem>, bookmarked: Boolean) {
        destroyActionModeIfNeeded()
        presenter.bookmarkChapters(chapters, bookmarked)
    }

    fun deleteChapters(chapters: List<ChapterItem>) {
        destroyActionModeIfNeeded()
//        DeletingChaptersDialog().show(childFragmentManager, DeletingChaptersDialog.TAG)
        presenter.deleteChapters(chapters)
    }

    fun onChaptersDeleted() {
        val adapter = adapter ?: return
        dismissDeletingDialog()
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    fun onChaptersDeletedError(error: Throwable) {
        dismissDeletingDialog()
        Timber.e(error)
    }

    fun dismissDeletingDialog() {
//        (childFragmentManager.findFragmentByTag(DeletingChaptersDialog.TAG) as? DialogFragment)
//                ?.dismissAllowingStateLoss()
    }

    override fun onItemClick(position: Int): Boolean {
        val adapter = adapter ?: return false
        val item = adapter.getItem(position) ?: return false
        if (actionMode != null && adapter.mode == FlexibleAdapter.MODE_MULTI) {
            toggleSelection(position)
            return true
        } else {
            openChapter(item.chapter)
            return false
        }
    }

    override fun onItemLongClick(position: Int) {
        if (actionMode == null)
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)

        toggleSelection(position)
    }

    fun onItemMenuClick(position: Int, item: MenuItem) {
        val adapter = adapter ?: return
        val chapter = adapter.getItem(position)?.let { listOf(it) } ?: return

        when (item.itemId) {
            R.id.action_download -> downloadChapters(chapter)
            R.id.action_bookmark -> bookmarkChapters(chapter, true)
            R.id.action_remove_bookmark -> bookmarkChapters(chapter, false)
            R.id.action_delete -> deleteChapters(chapter)
            R.id.action_mark_as_read -> markAsRead(chapter)
            R.id.action_mark_as_unread -> markAsUnread(chapter)
            R.id.action_mark_previous_as_read -> markPreviousAsRead(chapter[0])
        }
    }

    private fun toggleSelection(position: Int) {
        val adapter = adapter ?: return
        adapter.toggleSelection(position)

        val count = adapter.selectedItemCount
        if (count == 0) {
            actionMode?.finish()
        } else {
            actionMode?.invalidate()
        }
    }

}
