package eu.kanade.tachiyomi.ui.manga.chapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.getCoordinates
import eu.kanade.tachiyomi.util.snack
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.DeletingChaptersDialog
import kotlinx.android.synthetic.main.fragment_manga_chapters.*
import nucleus.factory.RequiresPresenter
import timber.log.Timber

@RequiresPresenter(ChaptersPresenter::class)
class ChaptersFragment : BaseRxFragment<ChaptersPresenter>(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    companion object {
        /**
         * Creates a new instance of this fragment.
         *
         * @return a new instance of [ChaptersFragment].
         */
        fun newInstance(): ChaptersFragment {
            return ChaptersFragment()
        }

    }

    /**
     * Adapter containing a list of chapters.
     */
    private lateinit var adapter: ChaptersAdapter

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manga_chapters, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        // Init RecyclerView and adapter
        adapter = ChaptersAdapter(this)

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recycler.setHasFixedSize(true)
//        TODO enable in a future commit
//        adapter.setFastScroller(fast_scroller, context.getResourceColor(R.attr.colorAccent))
//        adapter.toggleFastScroller()

        swipe_refresh.setOnRefreshListener { fetchChapters() }

        fab.setOnClickListener {
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

    override fun onResume() {
        // Check if animation view is visible
        if (reveal_view.visibility == View.VISIBLE) {
            // Show the unReveal effect
            val coordinates = fab.getCoordinates()
            reveal_view.hideRevealEffect(coordinates.x, coordinates.y, 1920)
        }
        super.onResume()
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
                activity.supportInvalidateOptionsMenu()
            }
            R.id.action_filter_read -> {
                item.isChecked = !item.isChecked
                presenter.setReadFilter(item.isChecked)
                activity.supportInvalidateOptionsMenu()
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
                activity.supportInvalidateOptionsMenu()
            }
            R.id.action_sort -> presenter.revertSortOrder()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onNextManga(manga: Manga) {
        // Set initial values
        activity.supportInvalidateOptionsMenu()
    }

    fun onNextChapters(chapters: List<ChapterItem>) {
        // If the list is empty, fetch chapters from source if the conditions are met
        // We use presenter chapters instead because they are always unfiltered
        if (presenter.chapters.isEmpty())
            initialFetchChapters()

        destroyActionModeIfNeeded()
        adapter.updateDataSet(chapters)
    }

    private fun initialFetchChapters() {
        // Only fetch if this view is from the catalog and it hasn't requested previously
        if (isCatalogueManga && !presenter.hasRequested) {
            fetchChapters()
        }
    }

    fun fetchChapters() {
        swipe_refresh.isRefreshing = true
        presenter.fetchChaptersFromSource()
    }

    fun onFetchChaptersDone() {
        swipe_refresh.isRefreshing = false
    }

    fun onFetchChaptersError(error: Throwable) {
        swipe_refresh.isRefreshing = false
        context.toast(error.message)
    }

    val isCatalogueManga: Boolean
        get() = (activity as MangaActivity).fromCatalogue

    fun openChapter(chapter: Chapter, hasAnimation: Boolean = false) {
        val intent = ReaderActivity.newIntent(activity, presenter.manga, chapter)
        if (hasAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    private fun showDisplayModeDialog() {
        // Get available modes, ids and the selected mode
        val modes = intArrayOf(R.string.show_title, R.string.show_chapter_number)
        val ids = intArrayOf(Manga.DISPLAY_NAME, Manga.DISPLAY_NUMBER)
        val selectedIndex = if (presenter.manga.displayMode == Manga.DISPLAY_NAME) 0 else 1

        MaterialDialog.Builder(activity)
                .title(R.string.action_display_mode)
                .items(modes.map { getString(it) })
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
        // Get available modes, ids and the selected mode
        val modes = intArrayOf(R.string.sort_by_source, R.string.sort_by_number)
        val ids = intArrayOf(Manga.SORTING_SOURCE, Manga.SORTING_NUMBER)
        val selectedIndex = if (presenter.manga.sorting == Manga.SORTING_SOURCE) 0 else 1

        MaterialDialog.Builder(activity)
                .title(R.string.sorting_mode)
                .items(modes.map { getString(it) })
                .itemsIds(ids)
                .itemsCallbackSingleChoice(selectedIndex) { _, itemView, _, _ ->
                    // Save the new sorting mode
                    presenter.setSorting(itemView.id)
                    true
                }
                .show()
    }

    private fun showDownloadDialog() {
        // Get available modes
        val modes = intArrayOf(R.string.download_1, R.string.download_5, R.string.download_10,
                R.string.download_unread, R.string.download_all)

        MaterialDialog.Builder(activity)
                .title(R.string.manga_download)
                .negativeText(android.R.string.cancel)
                .items(modes.map { getString(it) })
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
        return recycler.findViewHolderForItemId(chapter.id!!) as? ChapterHolder
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.chapter_selection, menu)
        adapter.mode = FlexibleAdapter.MODE_MULTI
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> selectAll()
            R.id.action_mark_as_read -> markAsRead(getSelectedChapters())
            R.id.action_mark_as_unread -> markAsUnread(getSelectedChapters())
            R.id.action_download -> downloadChapters(getSelectedChapters())
            R.id.action_delete -> {
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
        adapter.mode = FlexibleAdapter.MODE_SINGLE
        adapter.clearSelection()
        actionMode = null
    }

    fun getSelectedChapters(): List<ChapterItem> {
        return adapter.selectedPositions.map { adapter.getItem(it) }
    }

    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    fun selectAll() {
        adapter.selectAll()
        setContextTitle(adapter.selectedItemCount)
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
        val chapters = if (presenter.sortDescending()) adapter.items.reversed() else adapter.items
        val chapterPos = chapters.indexOf(chapter)
        if (chapterPos != -1) {
            presenter.markChaptersRead(chapters.take(chapterPos), true)
        }
    }

    fun downloadChapters(chapters: List<ChapterItem>) {
        destroyActionModeIfNeeded()
        presenter.downloadChapters(chapters)
        if (!presenter.manga.favorite){
            recycler.snack(getString(R.string.snack_add_to_library), Snackbar.LENGTH_INDEFINITE) {
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
        DeletingChaptersDialog().show(childFragmentManager, DeletingChaptersDialog.TAG)
        presenter.deleteChapters(chapters)
    }

    fun onChaptersDeleted() {
        dismissDeletingDialog()
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    fun onChaptersDeletedError(error: Throwable) {
        dismissDeletingDialog()
        Timber.e(error)
    }

    fun dismissDeletingDialog() {
        (childFragmentManager.findFragmentByTag(DeletingChaptersDialog.TAG) as? DialogFragment)
                ?.dismissAllowingStateLoss()
    }

    override fun onItemClick(position: Int): Boolean {
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
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)

        toggleSelection(position)
    }

    fun onItemMenuClick(position: Int, item: MenuItem) {
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
        adapter.toggleSelection(position)

        val count = adapter.selectedItemCount
        if (count == 0) {
            actionMode?.finish()
        } else {
            setContextTitle(count)
            actionMode?.invalidate()
        }
    }

    private fun setContextTitle(count: Int) {
        actionMode?.title = getString(R.string.label_selected, count)
    }
}
