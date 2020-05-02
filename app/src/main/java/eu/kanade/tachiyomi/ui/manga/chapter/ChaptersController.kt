package eu.kanade.tachiyomi.ui.manga.chapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.databinding.ChaptersControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.main.offsetFabAppbarHeight
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.getCoordinates
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.shrinkOnScroll
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import timber.log.Timber

class ChaptersController :
    NucleusController<ChaptersControllerBinding, ChaptersPresenter>(),
    ActionMode.Callback,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
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

    private var lastClickPosition = -1

    init {
        setHasOptionsMenu(true)
        setOptionsMenuHidden(true)
    }

    override fun createPresenter(): ChaptersPresenter {
        val ctrl = parentController as MangaController
        return ChaptersPresenter(
            ctrl.manga!!, ctrl.source!!,
            ctrl.chapterCountRelay, ctrl.lastUpdateRelay, ctrl.mangaFavoriteRelay
        )
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = ChaptersControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Init RecyclerView and adapter
        adapter = ChaptersAdapter(this, view.context)

        binding.recycler.adapter = adapter
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        binding.recycler.setHasFixedSize(true)
        adapter?.fastScroller = binding.fastScroller

        binding.swipeRefresh.refreshes()
            .onEach { fetchChaptersFromSource() }
            .launchIn(scope)

        binding.fab.clicks()
            .onEach {
                val item = presenter.getNextUnreadChapter()
                if (item != null) {
                    // Create animation listener
                    val revealAnimationListener: Animator.AnimatorListener = object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            openChapter(item.chapter, true)
                        }
                    }

                    // Get coordinates and start animation
                    val coordinates = binding.fab.getCoordinates()
                    if (!binding.revealView.showRevealEffect(coordinates.x, coordinates.y, revealAnimationListener)) {
                        openChapter(item.chapter)
                    }
                } else {
                    view.context.toast(R.string.no_next_chapter)
                }
            }
            .launchIn(scope)

        binding.fab.offsetFabAppbarHeight(activity!!)
        binding.fab.shrinkOnScroll(binding.recycler)
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        binding.actionToolbar.destroy()
        adapter = null
        super.onDestroyView(view)
    }

    override fun onActivityResumed(activity: Activity) {
        if (view == null) return

        // Check if animation view is visible
        if (binding.revealView.visibility == View.VISIBLE) {
            // Show the unreveal effect
            val coordinates = binding.fab.getCoordinates()
            binding.revealView.hideRevealEffect(coordinates.x, coordinates.y, 1920)
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
        val menuFilterEmpty = menu.findItem(R.id.action_filter_empty)

        // Set correct checkbox values.
        menuFilterRead.isChecked = presenter.onlyRead()
        menuFilterUnread.isChecked = presenter.onlyUnread()
        menuFilterDownloaded.isChecked = presenter.onlyDownloaded()
        menuFilterDownloaded.isEnabled = !presenter.forceDownloaded()
        menuFilterBookmarked.isChecked = presenter.onlyBookmarked()

        val filterSet = presenter.onlyRead() || presenter.onlyUnread() || presenter.onlyDownloaded() || presenter.onlyBookmarked()

        if (filterSet) {
            val filterColor = activity!!.getResourceColor(R.attr.colorFilterActive)
            DrawableCompat.setTint(menu.findItem(R.id.action_filter).icon, filterColor)
        }

        // Only show remove filter option if there's a filter set.
        menuFilterEmpty.isVisible = filterSet

        // Disable unread filter option if read filter is enabled.
        if (presenter.onlyRead()) {
            menuFilterUnread.isEnabled = false
        }
        // Disable read filter option if unread filter is enabled.
        if (presenter.onlyUnread()) {
            menuFilterRead.isEnabled = false
        }

        // Display mode submenu
        if (presenter.manga.displayMode == Manga.DISPLAY_NAME) {
            menu.findItem(R.id.display_title).isChecked = true
        } else {
            menu.findItem(R.id.display_chapter_number).isChecked = true
        }

        // Sorting mode submenu
        if (presenter.manga.sorting == Manga.SORTING_SOURCE) {
            menu.findItem(R.id.sort_by_source).isChecked = true
        } else {
            menu.findItem(R.id.sort_by_number).isChecked = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.display_title -> {
                item.isChecked = true
                setDisplayMode(Manga.DISPLAY_NAME)
            }
            R.id.display_chapter_number -> {
                item.isChecked = true
                setDisplayMode(Manga.DISPLAY_NUMBER)
            }

            R.id.sort_by_source -> {
                item.isChecked = true
                presenter.setSorting(Manga.SORTING_SOURCE)
            }
            R.id.sort_by_number -> {
                item.isChecked = true
                presenter.setSorting(Manga.SORTING_NUMBER)
            }

            R.id.download_next, R.id.download_next_5, R.id.download_next_10,
            R.id.download_custom, R.id.download_unread, R.id.download_all
            -> downloadChapters(item.itemId)

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
                activity?.invalidateOptionsMenu()
            }
            R.id.action_filter_bookmarked -> {
                item.isChecked = !item.isChecked
                presenter.setBookmarkedFilter(item.isChecked)
                activity?.invalidateOptionsMenu()
            }
            R.id.action_filter_empty -> {
                presenter.removeFilters()
                activity?.invalidateOptionsMenu()
            }
            R.id.action_sort -> presenter.revertSortOrder()
        }
        return super.onOptionsItemSelected(item)
    }

    fun onNextChapters(chapters: List<ChapterItem>) {
        // If the list is empty, fetch chapters from source if the conditions are met
        // We use presenter chapters instead because they are always unfiltered
        if (presenter.chapters.isEmpty()) {
            initialFetchChapters()
        }

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
        if ((parentController as MangaController).fromSource && !presenter.hasRequested) {
            fetchChaptersFromSource()
        }
    }

    private fun fetchChaptersFromSource() {
        binding.swipeRefresh.isRefreshing = true
        presenter.fetchChaptersFromSource()
    }

    fun onFetchChaptersDone() {
        binding.swipeRefresh.isRefreshing = false
    }

    fun onFetchChaptersError(error: Throwable) {
        binding.swipeRefresh.isRefreshing = false
        activity?.toast(error.message)
    }

    fun onChapterStatusChange(download: Download) {
        getHolder(download.chapter)?.notifyStatus(download.status)
    }

    private fun getHolder(chapter: Chapter): ChapterHolder? {
        return binding.recycler.findViewHolderForItemId(chapter.id!!) as? ChapterHolder
    }

    fun openChapter(chapter: Chapter, hasAnimation: Boolean = false) {
        val activity = activity ?: return
        val intent = ReaderActivity.newIntent(activity, presenter.manga, chapter)
        if (hasAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val adapter = adapter ?: return false
        val item = adapter.getItem(position) ?: return false
        return if (actionMode != null && adapter.mode == SelectableAdapter.Mode.MULTI) {
            lastClickPosition = position
            toggleSelection(position)
            true
        } else {
            openChapter(item.chapter)
            false
        }
    }

    override fun onItemLongClick(position: Int) {
        createActionModeIfNeeded()
        when {
            lastClickPosition == -1 -> setSelection(position)
            lastClickPosition > position ->
                for (i in position until lastClickPosition)
                    setSelection(i)
            lastClickPosition < position ->
                for (i in lastClickPosition + 1..position)
                    setSelection(i)
            else -> setSelection(position)
        }
        lastClickPosition = position
        adapter?.notifyDataSetChanged()
    }

    // SELECTIONS & ACTION MODE

    private fun toggleSelection(position: Int) {
        val adapter = adapter ?: return
        val item = adapter.getItem(position) ?: return
        adapter.toggleSelection(position)
        adapter.notifyDataSetChanged()
        if (adapter.isSelected(position)) {
            selectedItems.add(item)
        } else {
            selectedItems.remove(item)
        }
        actionMode?.invalidate()
    }

    private fun setSelection(position: Int) {
        val adapter = adapter ?: return
        val item = adapter.getItem(position) ?: return
        if (!adapter.isSelected(position)) {
            adapter.toggleSelection(position)
            selectedItems.add(item)
            actionMode?.invalidate()
        }
    }

    private fun getSelectedChapters(): List<ChapterItem> {
        val adapter = adapter ?: return emptyList()
        return adapter.selectedPositions.mapNotNull { adapter.getItem(it) }
    }

    private fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
            binding.actionToolbar.show(
                actionMode!!,
                R.menu.chapter_selection
            ) { onActionItemClicked(actionMode!!, it!!) }
        }
    }

    private fun destroyActionModeIfNeeded() {
        lastClickPosition = -1
        actionMode?.finish()
    }

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
            binding.actionToolbar.findItem(R.id.action_download)?.isVisible = chapters.any { !it.isDownloaded }
            binding.actionToolbar.findItem(R.id.action_delete)?.isVisible = chapters.any { it.isDownloaded }
            binding.actionToolbar.findItem(R.id.action_bookmark)?.isVisible = chapters.any { !it.chapter.bookmark }
            binding.actionToolbar.findItem(R.id.action_remove_bookmark)?.isVisible = chapters.all { it.chapter.bookmark }
            binding.actionToolbar.findItem(R.id.action_mark_as_read)?.isVisible = chapters.any { !it.chapter.read }
            binding.actionToolbar.findItem(R.id.action_mark_as_unread)?.isVisible = chapters.all { it.chapter.read }

            // Hide FAB to avoid interfering with the bottom action toolbar
            // binding.fab.hide()
            binding.fab.gone()
        }
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> selectAll()
            R.id.action_select_inverse -> selectInverse()
            R.id.action_download -> downloadChapters(getSelectedChapters())
            R.id.action_delete -> showDeleteChaptersConfirmationDialog()
            R.id.action_bookmark -> bookmarkChapters(getSelectedChapters(), true)
            R.id.action_remove_bookmark -> bookmarkChapters(getSelectedChapters(), false)
            R.id.action_mark_as_read -> markAsRead(getSelectedChapters())
            R.id.action_mark_as_unread -> markAsUnread(getSelectedChapters())
            R.id.action_mark_previous_as_read -> markPreviousAsRead(getSelectedChapters())
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        binding.actionToolbar.hide()
        adapter?.mode = SelectableAdapter.Mode.SINGLE
        adapter?.clearSelection()
        selectedItems.clear()
        actionMode = null

        // TODO: there seems to be a bug in MaterialComponents where the [ExtendedFloatingActionButton]
        // fails to show up properly
        // binding.fab.show()
        binding.fab.visible()
    }

    override fun onDetach(view: View) {
        destroyActionModeIfNeeded()
        super.onDetach(view)
    }

    // SELECTION MODE ACTIONS

    private fun selectAll() {
        val adapter = adapter ?: return
        adapter.selectAll()
        selectedItems.addAll(adapter.items)
        actionMode?.invalidate()
    }

    private fun selectInverse() {
        val adapter = adapter ?: return

        selectedItems.clear()
        for (i in 0..adapter.itemCount) {
            adapter.toggleSelection(i)
        }
        selectedItems.addAll(adapter.selectedPositions.mapNotNull { adapter.getItem(it) })

        actionMode?.invalidate()
        adapter.notifyDataSetChanged()
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
        presenter.downloadChapters(chapters)
        if (view != null && !presenter.manga.favorite) {
            binding.recycler.snack(view.context.getString(R.string.snack_add_to_library), Snackbar.LENGTH_INDEFINITE) {
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

    private fun markPreviousAsRead(chapters: List<ChapterItem>) {
        val adapter = adapter ?: return
        val prevChapters = if (presenter.sortDescending()) adapter.items.reversed() else adapter.items
        val chapterPos = prevChapters.indexOf(chapters.last())
        if (chapterPos != -1) {
            markAsRead(prevChapters.take(chapterPos))
        }
    }

    private fun bookmarkChapters(chapters: List<ChapterItem>, bookmarked: Boolean) {
        presenter.bookmarkChapters(chapters, bookmarked)
    }

    fun deleteChapters(chapters: List<ChapterItem>) {
        if (chapters.isEmpty()) return

        presenter.deleteChapters(chapters)
    }

    fun onChaptersDeleted(chapters: List<ChapterItem>) {
        // this is needed so the downloaded text gets removed from the item
        chapters.forEach {
            adapter?.updateItem(it)
        }
        adapter?.notifyDataSetChanged()
    }

    fun onChaptersDeletedError(error: Throwable) {
        Timber.e(error)
    }

    // OVERFLOW MENU DIALOGS

    private fun setDisplayMode(id: Int) {
        presenter.setDisplayMode(id)
        adapter?.notifyDataSetChanged()
    }

    private fun getUnreadChaptersSorted() = presenter.chapters
        .filter { !it.read && it.status == Download.NOT_DOWNLOADED }
        .distinctBy { it.name }
        .sortedByDescending { it.source_order }

    private fun downloadChapters(choice: Int) {
        val chaptersToDownload = when (choice) {
            R.id.download_next -> getUnreadChaptersSorted().take(1)
            R.id.download_next_5 -> getUnreadChaptersSorted().take(5)
            R.id.download_next_10 -> getUnreadChaptersSorted().take(10)
            R.id.download_custom -> {
                showCustomDownloadDialog()
                return
            }
            R.id.download_unread -> presenter.chapters.filter { !it.read }
            R.id.download_all -> presenter.chapters
            else -> emptyList()
        }
        if (chaptersToDownload.isNotEmpty()) {
            downloadChapters(chaptersToDownload)
        }
    }

    private fun showCustomDownloadDialog() {
        DownloadCustomChaptersDialog(this, presenter.chapters.size).showDialog(router)
    }

    override fun downloadCustomChapters(amount: Int) {
        val chaptersToDownload = getUnreadChaptersSorted().take(amount)
        if (chaptersToDownload.isNotEmpty()) {
            downloadChapters(chaptersToDownload)
        }
    }
}
