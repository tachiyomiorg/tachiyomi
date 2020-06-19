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
import androidx.recyclerview.widget.MergeAdapter
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ChaptersControllerBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.migration.search.SearchController
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchController
import eu.kanade.tachiyomi.ui.library.ChangeMangaCategoriesDialog
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.offsetAppbarHeight
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.recent.history.HistoryController
import eu.kanade.tachiyomi.ui.recent.updates.UpdatesController
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
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
import uy.kohesive.injekt.injectLazy

class MangaInfoChaptersController(private val fromSource: Boolean = false) :
    NucleusController<ChaptersControllerBinding, MangaInfoChaptersPresenter>(),
    ActionMode.Callback,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    ChangeMangaCategoriesDialog.Listener,
    DownloadCustomChaptersDialog.Listener,
    DeleteChaptersDialog.Listener {

    private val preferences: PreferencesHelper by injectLazy()

    private var headerAdapter: MangaInfoHeaderAdapter? = null
    private var chaptersAdapter: ChaptersAdapter? = null

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    /**
     * Selected items. Used to restore selections after a rotation.
     */
    private val selectedChapters = mutableSetOf<ChapterItem>()

    private var lastClickPosition = -1

    private var isRefreshingInfo = false
    private var isRefreshingChapters = false

    init {
        setHasOptionsMenu(true)
        setOptionsMenuHidden(true)
    }

    override fun createPresenter(): MangaInfoChaptersPresenter {
        val ctrl = parentController as MangaController
        return MangaInfoChaptersPresenter(
            ctrl.manga!!, ctrl.source!!, ctrl.mangaFavoriteRelay
        )
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = ChaptersControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        val ctrl = parentController as MangaController
        if (ctrl.manga == null || ctrl.source == null) return

        // Init RecyclerView and adapter
        headerAdapter = MangaInfoHeaderAdapter(this, fromSource)
        chaptersAdapter = ChaptersAdapter(this, view.context)

        binding.recycler.adapter = MergeAdapter(headerAdapter, chaptersAdapter)
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        binding.recycler.setHasFixedSize(true)
        chaptersAdapter?.fastScroller = binding.fastScroller

        binding.swipeRefresh.refreshes()
            .onEach {
                fetchMangaInfoFromSource(manualFetch = true)
                fetchChaptersFromSource(manualFetch = true)
            }
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

        binding.fab.shrinkOnScroll(binding.recycler)

        binding.actionToolbar.offsetAppbarHeight(activity!!)
        binding.fab.offsetAppbarHeight(activity!!)
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        binding.actionToolbar.destroy()
        headerAdapter = null
        chaptersAdapter = null
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
        val sortingItem = when (presenter.manga.sorting) {
            Manga.SORTING_SOURCE -> R.id.sort_by_source
            Manga.SORTING_NUMBER -> R.id.sort_by_number
            Manga.SORTING_UPLOAD_DATE -> R.id.sort_by_upload_date
            else -> throw NotImplementedError("Unimplemented sorting method")
        }
        menu.findItem(sortingItem).isChecked = true
        menu.findItem(R.id.action_sort_descending).isChecked = presenter.manga.sortDescending()
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
            R.id.sort_by_upload_date -> {
                item.isChecked = true
                presenter.setSorting(Manga.SORTING_UPLOAD_DATE)
            }
            R.id.action_sort_descending -> {
                presenter.reverseSortOrder()
                activity?.invalidateOptionsMenu()
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

            R.id.action_migrate -> migrateManga()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateRefreshing() {
        binding.swipeRefresh.isRefreshing = isRefreshingInfo || isRefreshingChapters
    }

    // Manga info - start

    /**
     * Check if manga is initialized.
     * If true update header with manga information,
     * if false fetch manga information
     *
     * @param manga manga object containing information about manga.
     * @param source the source of the manga.
     */
    fun onNextMangaInfo(manga: Manga, source: Source) {
        if (manga.initialized) {
            // Update view.
            headerAdapter?.update(manga, source)
        } else {
            // Initialize manga.
            fetchMangaInfoFromSource()
        }
    }

    /**
     * Start fetching manga information from source.
     */
    private fun fetchMangaInfoFromSource(manualFetch: Boolean = false) {
        isRefreshingInfo = true
        updateRefreshing()

        // Call presenter and start fetching manga information
        presenter.fetchMangaFromSource(manualFetch)
    }

    fun onFetchMangaInfoDone() {
        isRefreshingInfo = false
        updateRefreshing()
    }

    fun onFetchMangaInfoError(error: Throwable) {
        isRefreshingInfo = false
        updateRefreshing()
        activity?.toast(error.message)
    }

    fun openMangaInWebView() {
        val source = presenter.source as? HttpSource ?: return

        val url = try {
            source.mangaDetailsRequest(presenter.manga).url.toString()
        } catch (e: Exception) {
            return
        }

        val activity = activity ?: return
        val intent = WebViewActivity.newIntent(activity, url, source.id, presenter.manga.title)
        startActivity(intent)
    }

    fun shareManga() {
        val context = view?.context ?: return

        val source = presenter.source as? HttpSource ?: return
        try {
            val url = source.mangaDetailsRequest(presenter.manga).url.toString()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    fun onFavoriteClick() {
        val manga = presenter.manga

        if (manga.favorite) {
            toggleFavorite()
            activity?.toast(activity?.getString(R.string.manga_removed_library))
        } else {
            val categories = presenter.getCategories()
            val defaultCategoryId = preferences.defaultCategory()
            val defaultCategory = categories.find { it.id == defaultCategoryId }

            when {
                // Default category set
                defaultCategory != null -> {
                    toggleFavorite()
                    presenter.moveMangaToCategory(manga, defaultCategory)
                    activity?.toast(activity?.getString(R.string.manga_added_library))
                }

                // Automatic 'Default' or no categories
                defaultCategoryId == 0 || categories.isEmpty() -> {
                    toggleFavorite()
                    presenter.moveMangaToCategory(manga, null)
                    activity?.toast(activity?.getString(R.string.manga_added_library))
                }

                // Choose a category
                else -> {
                    val ids = presenter.getMangaCategoryIds(manga)
                    val preselected = ids.mapNotNull { id ->
                        categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
                    }.toTypedArray()

                    ChangeMangaCategoriesDialog(this, listOf(manga), categories, preselected)
                        .showDialog(router)
                }
            }
        }
    }

    /**
     * Toggles the favorite status and asks for confirmation to delete downloaded chapters.
     */
    private fun toggleFavorite() {
        val view = view

        val isNowFavorite = presenter.toggleFavorite()
        if (view != null && !isNowFavorite && presenter.hasDownloads()) {
            view.snack(view.context.getString(R.string.delete_downloads_for_manga)) {
                setAction(R.string.action_delete) {
                    presenter.deleteDownloads()
                }
            }
        }

        headerAdapter?.notifyDataSetChanged()
    }

    fun onCategoriesClick() {
        val manga = presenter.manga
        val categories = presenter.getCategories()

        val ids = presenter.getMangaCategoryIds(manga)
        val preselected = ids.mapNotNull { id ->
            categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
        }.toTypedArray()

        ChangeMangaCategoriesDialog(this, listOf(manga), categories, preselected)
            .showDialog(router)
    }

    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        val manga = mangas.firstOrNull() ?: return

        if (!manga.favorite) {
            toggleFavorite()
            activity?.toast(activity?.getString(R.string.manga_added_library))
        }

        presenter.moveMangaToCategories(manga, categories)
    }

    /**
     * Perform a global search using the provided query.
     *
     * @param query the search query to pass to the search controller
     */
    fun performGlobalSearch(query: String) {
        val router = parentController?.router ?: return
        router.pushController(GlobalSearchController(query).withFadeTransaction())
    }

    /**
     * Perform a search using the provided query.
     *
     * @param query the search query to the parent controller
     */
    fun performSearch(query: String) {
        val router = parentController?.router ?: return

        if (router.backstackSize < 2) {
            return
        }

        when (val previousController = router.backstack[router.backstackSize - 2].controller()) {
            is LibraryController -> {
                router.handleBack()
                previousController.search(query)
            }
            is UpdatesController,
            is HistoryController -> {
                // Manually navigate to LibraryController
                router.handleBack()
                (router.activity as MainActivity).setSelectedNavItem(R.id.nav_library)
                val controller = router.getControllerWithTag(R.id.nav_library.toString()) as LibraryController
                controller.search(query)
            }
            is BrowseSourceController -> {
                router.handleBack()
                previousController.searchWithQuery(query)
            }
        }
    }

    // Manga info - end

    // Chapters list - start

    /**
     * Initiates source migration for the specific manga.
     */
    private fun migrateManga() {
        val controller =
            SearchController(
                presenter.manga
            )
        controller.targetController = this
        parentController!!.router.pushController(controller.withFadeTransaction())
    }

    fun onNextChapters(chapters: List<ChapterItem>) {
        // If the list is empty and it hasn't requested previously, fetch chapters from source
        // We use presenter chapters instead because they are always unfiltered
        if (!presenter.hasRequested && presenter.chapters.isEmpty()) {
            fetchChaptersFromSource()
        }

        val adapter = chaptersAdapter ?: return
        adapter.updateDataSet(chapters)

        if (selectedChapters.isNotEmpty()) {
            adapter.clearSelection() // we need to start from a clean state, index may have changed
            createActionModeIfNeeded()
            selectedChapters.forEach { item ->
                val position = adapter.indexOf(item)
                if (position != -1 && !adapter.isSelected(position)) {
                    adapter.toggleSelection(position)
                }
            }
            actionMode?.invalidate()
        }

        val context = view?.context
        if (context != null && chapters.any { it.read }) {
            binding.fab.text = context.getString(R.string.action_resume)
        }
    }

    private fun fetchChaptersFromSource(manualFetch: Boolean = false) {
        isRefreshingChapters = true
        updateRefreshing()

        presenter.fetchChaptersFromSource(manualFetch)
    }

    fun onFetchChaptersDone() {
        isRefreshingChapters = false
        updateRefreshing()
    }

    fun onFetchChaptersError(error: Throwable) {
        isRefreshingChapters = false
        updateRefreshing()
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
        val adapter = chaptersAdapter ?: return false
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
        chaptersAdapter?.notifyDataSetChanged()
    }

    // SELECTIONS & ACTION MODE

    private fun toggleSelection(position: Int) {
        val adapter = chaptersAdapter ?: return
        val item = adapter.getItem(position) ?: return
        adapter.toggleSelection(position)
        adapter.notifyDataSetChanged()
        if (adapter.isSelected(position)) {
            selectedChapters.add(item)
        } else {
            selectedChapters.remove(item)
        }
        actionMode?.invalidate()
    }

    private fun setSelection(position: Int) {
        val adapter = chaptersAdapter ?: return
        val item = adapter.getItem(position) ?: return
        if (!adapter.isSelected(position)) {
            adapter.toggleSelection(position)
            selectedChapters.add(item)
            actionMode?.invalidate()
        }
    }

    private fun getSelectedChapters(): List<ChapterItem> {
        val adapter = chaptersAdapter ?: return emptyList()
        return adapter.selectedPositions.mapNotNull { adapter.getItem(it) }
    }

    private fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
            binding.actionToolbar.show(
                actionMode!!,
                R.menu.chapter_selection
            ) { onActionItemClicked(it!!) }
        }
    }

    private fun destroyActionModeIfNeeded() {
        lastClickPosition = -1
        actionMode?.finish()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.generic_selection, menu)
        chaptersAdapter?.mode = SelectableAdapter.Mode.MULTI
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = chaptersAdapter?.selectedItemCount ?: 0
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = count.toString()

            val isLocalSource = presenter.source.id == LocalSource.ID
            val chapters = getSelectedChapters()
            binding.actionToolbar.findItem(R.id.action_download)?.isVisible = !isLocalSource && chapters.any { !it.isDownloaded }
            binding.actionToolbar.findItem(R.id.action_delete)?.isVisible = !isLocalSource && chapters.any { it.isDownloaded }
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
        return onActionItemClicked(item)
    }

    private fun onActionItemClicked(item: MenuItem): Boolean {
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
        chaptersAdapter?.mode = SelectableAdapter.Mode.SINGLE
        chaptersAdapter?.clearSelection()
        selectedChapters.clear()
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
        val adapter = chaptersAdapter ?: return
        adapter.selectAll()
        selectedChapters.addAll(adapter.items)
        actionMode?.invalidate()
    }

    private fun selectInverse() {
        val adapter = chaptersAdapter ?: return

        selectedChapters.clear()
        for (i in 0..adapter.itemCount) {
            adapter.toggleSelection(i)
        }
        selectedChapters.addAll(adapter.selectedPositions.mapNotNull { adapter.getItem(it) })

        actionMode?.invalidate()
        adapter.notifyDataSetChanged()
    }

    private fun markAsRead(chapters: List<ChapterItem>) {
        presenter.markChaptersRead(chapters, true)
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            deleteChapters(chapters)
        }
        destroyActionModeIfNeeded()
    }

    private fun markAsUnread(chapters: List<ChapterItem>) {
        presenter.markChaptersRead(chapters, false)
        destroyActionModeIfNeeded()
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
        destroyActionModeIfNeeded()
    }

    private fun showDeleteChaptersConfirmationDialog() {
        DeleteChaptersDialog(this).showDialog(router)
    }

    override fun deleteChapters() {
        deleteChapters(getSelectedChapters())
    }

    private fun markPreviousAsRead(chapters: List<ChapterItem>) {
        val adapter = chaptersAdapter ?: return
        val prevChapters = if (presenter.sortDescending()) adapter.items.reversed() else adapter.items
        val chapterPos = prevChapters.indexOf(chapters.last())
        if (chapterPos != -1) {
            markAsRead(prevChapters.take(chapterPos))
        }
        destroyActionModeIfNeeded()
    }

    private fun bookmarkChapters(chapters: List<ChapterItem>, bookmarked: Boolean) {
        presenter.bookmarkChapters(chapters, bookmarked)
        destroyActionModeIfNeeded()
    }

    fun deleteChapters(chapters: List<ChapterItem>) {
        if (chapters.isEmpty()) return

        presenter.deleteChapters(chapters)
        destroyActionModeIfNeeded()
    }

    fun onChaptersDeleted(chapters: List<ChapterItem>) {
        // this is needed so the downloaded text gets removed from the item
        chapters.forEach {
            chaptersAdapter?.updateItem(it)
        }
        chaptersAdapter?.notifyDataSetChanged()
    }

    fun onChaptersDeletedError(error: Throwable) {
        Timber.e(error)
    }

    // OVERFLOW MENU DIALOGS

    private fun setDisplayMode(id: Int) {
        presenter.setDisplayMode(id)
        chaptersAdapter?.notifyDataSetChanged()
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
        destroyActionModeIfNeeded()
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

    // Chapters list - end
}
