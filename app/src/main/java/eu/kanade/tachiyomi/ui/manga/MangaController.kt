package eu.kanade.tachiyomi.ui.manga

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.UnattendedTrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.MangaControllerBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.controller.FabController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.ToolbarLiftOnScrollController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.migration.search.SearchController
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchController
import eu.kanade.tachiyomi.ui.browse.source.latest.LatestUpdatesController
import eu.kanade.tachiyomi.ui.library.ChangeMangaCategoriesDialog
import eu.kanade.tachiyomi.ui.library.ChangeMangaCoverDialog
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersAdapter
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersSettingsSheet
import eu.kanade.tachiyomi.ui.manga.chapter.DeleteChaptersDialog
import eu.kanade.tachiyomi.ui.manga.chapter.DownloadCustomChaptersDialog
import eu.kanade.tachiyomi.ui.manga.chapter.MangaChaptersHeaderAdapter
import eu.kanade.tachiyomi.ui.manga.chapter.base.BaseChaptersAdapter
import eu.kanade.tachiyomi.ui.manga.info.MangaInfoHeaderAdapter
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.ui.manga.track.TrackSearchDialog
import eu.kanade.tachiyomi.ui.manga.track.TrackSheet
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.recent.history.HistoryController
import eu.kanade.tachiyomi.ui.recent.updates.UpdatesController
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.chapter.NoChaptersException
import eu.kanade.tachiyomi.util.hasCustomCover
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.getCoordinates
import eu.kanade.tachiyomi.util.view.shrinkOnScroll
import eu.kanade.tachiyomi.util.view.snack
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.recyclerview.scrollEvents
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.ArrayDeque
import kotlin.math.min

class MangaController :
    NucleusController<MangaControllerBinding, MangaPresenter>,
    ToolbarLiftOnScrollController,
    FabController,
    ActionMode.Callback,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    BaseChaptersAdapter.OnChapterClickListener,
    ChangeMangaCoverDialog.Listener,
    ChangeMangaCategoriesDialog.Listener,
    DownloadCustomChaptersDialog.Listener,
    DeleteChaptersDialog.Listener {

    constructor(manga: Manga?, fromSource: Boolean = false) : super(
        bundleOf(
            MANGA_EXTRA to (manga?.id ?: 0),
            FROM_SOURCE_EXTRA to fromSource
        )
    ) {
        this.manga = manga
        if (manga != null) {
            source = Injekt.get<SourceManager>().getOrStub(manga.source)
        }
    }

    constructor(mangaId: Long) : this(
        Injekt.get<DatabaseHelper>().getManga(mangaId).executeAsBlocking()
    )

    @Suppress("unused")
    constructor(bundle: Bundle) : this(bundle.getLong(MANGA_EXTRA))

    var manga: Manga? = null
        private set

    var source: Source? = null
        private set

    val fromSource = args.getBoolean(FROM_SOURCE_EXTRA, false)

    private val preferences: PreferencesHelper by injectLazy()
    private val coverCache: CoverCache by injectLazy()

    private var toolbarTextView: TextView? = null

    private var mangaInfoAdapter: MangaInfoHeaderAdapter? = null
    private var chaptersHeaderAdapter: MangaChaptersHeaderAdapter? = null
    private var chaptersAdapter: ChaptersAdapter? = null

    // Sheet containing filter/sort/display items.
    private var settingsSheet: ChaptersSettingsSheet? = null

    private var actionFab: ExtendedFloatingActionButton? = null
    private var actionFabScrollListener: RecyclerView.OnScrollListener? = null

    // Snackbar to add manga to library after downloading chapter(s)
    private var addSnackbar: Snackbar? = null

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    /**
     * Selected items. Used to restore selections after a rotation.
     */
    private val selectedChapters = mutableSetOf<ChapterItem>()

    private val isLocalSource by lazy { presenter.source.id == LocalSource.ID }

    private var lastClickPositionStack = ArrayDeque(listOf(-1))

    private var isRefreshingInfo = false
    private var isRefreshingChapters = false

    private var trackSheet: TrackSheet? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return manga?.title
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)

        // Hide toolbar title on enter
        if (type.isEnter) {
            updateToolbarTitleAlpha()
        }
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (manga == null || source == null) {
            activity?.toast(R.string.manga_not_in_db)
            router.popController(this)
        }
    }

    override fun createPresenter(): MangaPresenter {
        return MangaPresenter(
            manga!!,
            source!!
        )
    }

    override fun createBinding(inflater: LayoutInflater) = MangaControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        listOfNotNull(binding.fullRecycler, binding.infoRecycler, binding.chaptersRecycler)
            .forEach {
                it.applyInsetter {
                    type(navigationBars = true) {
                        padding()
                    }
                }

                it.layoutManager = LinearLayoutManager(view.context)
                it.setHasFixedSize(true)
            }
        binding.actionToolbar.applyInsetter {
            type(navigationBars = true) {
                margin(bottom = true)
            }
        }

        if (manga == null || source == null) return

        // Init RecyclerView and adapter
        mangaInfoAdapter = MangaInfoHeaderAdapter(this, fromSource, binding.infoRecycler != null)
        chaptersHeaderAdapter = MangaChaptersHeaderAdapter(this)
        chaptersAdapter = ChaptersAdapter(this, view.context)

        // Phone layout
        binding.fullRecycler?.let {
            it.adapter = ConcatAdapter(mangaInfoAdapter, chaptersHeaderAdapter, chaptersAdapter)

            it.scrollEvents()
                .onEach { updateToolbarTitleAlpha() }
                .launchIn(viewScope)

            // Skips directly to chapters list if navigated to from the library
            it.post {
                if (!fromSource && preferences.jumpToChapters()) {
                    (it.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(1, 0)
                }

                // Delayed in case we need to jump to chapters
                it.post {
                    updateToolbarTitleAlpha()
                }
            }
        }
        // Tablet layout
        binding.infoRecycler?.let {
            it.adapter = mangaInfoAdapter

            it.scrollEvents()
                .onEach { updateToolbarTitleAlpha() }
                .launchIn(viewScope)

            // Delayed in case we need to jump to chapters
            it.post {
                updateToolbarTitleAlpha()
            }
        }
        binding.chaptersRecycler?.let {
            it.adapter = ConcatAdapter(chaptersHeaderAdapter, chaptersAdapter)
        }

        chaptersAdapter?.fastScroller = binding.fastScroller

        actionFabScrollListener = actionFab?.shrinkOnScroll(chapterRecycler)
        // Initially set FAB invisible; will become visible if unread chapters are present
        actionFab?.isVisible = false

        binding.swipeRefresh.refreshes()
            .onEach {
                fetchMangaInfoFromSource(manualFetch = true)
                fetchChaptersFromSource(manualFetch = true)
            }
            .launchIn(viewScope)

        (activity as? MainActivity)?.fixViewToBottom(binding.actionToolbar)

        settingsSheet = ChaptersSettingsSheet(router, presenter) { group ->
            if (group is ChaptersSettingsSheet.Filter.FilterGroup) {
                updateFilterIconState()
                chaptersAdapter?.notifyDataSetChanged()
            }
        }

        trackSheet = TrackSheet(this, manga!!, (activity as MainActivity).supportFragmentManager)

        updateFilterIconState()
    }

    private fun updateToolbarTitleAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float? = null) {
        val scrolledList = binding.fullRecycler ?: binding.infoRecycler!!
        if (toolbarTextView == null) {
            toolbarTextView = (activity as? MainActivity)?.binding?.toolbar?.children
                ?.find { it is TextView } as? TextView
        }
        toolbarTextView?.alpha = when {
            // Specific alpha provided
            alpha != null -> alpha

            // First item isn't in view, full opacity
            ((scrolledList.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > 0) -> 1F

            // Based on scroll amount when first item is in view
            else -> min(scrolledList.computeVerticalScrollOffset(), 255) / 255F
        }
    }

    private fun updateFilterIconState() {
        chaptersHeaderAdapter?.setHasActiveFilters(settingsSheet?.filters?.hasActiveFilters() == true)
    }

    override fun configureFab(fab: ExtendedFloatingActionButton) {
        actionFab = fab
        fab.setText(R.string.action_start)
        fab.setIconResource(R.drawable.ic_play_arrow_24dp)
        fab.setOnClickListener {
            val item = presenter.getNextUnreadChapter()
            if (item != null) {
                // Get coordinates and start animation
                actionFab?.getCoordinates()?.let { coordinates ->
                    binding.revealView.showRevealEffect(
                        coordinates.x,
                        coordinates.y,
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator?) {
                                openChapter(item.chapter, true)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun cleanupFab(fab: ExtendedFloatingActionButton) {
        fab.setOnClickListener(null)
        actionFabScrollListener?.let { chapterRecycler.removeOnScrollListener(it) }
        actionFab = null
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        (activity as? MainActivity)?.clearFixViewToBottom(binding.actionToolbar)
        binding.actionToolbar.destroy()
        mangaInfoAdapter = null
        chaptersHeaderAdapter = null
        chaptersAdapter = null
        settingsSheet = null
        addSnackbar?.dismiss()
        updateToolbarTitleAlpha(1F)
        toolbarTextView = null
        super.onDestroyView(view)
    }

    override fun onActivityResumed(activity: Activity) {
        if (view == null) return

        // Check if animation view is visible
        if (binding.revealView.isVisible) {
            // Show the unreveal effect
            actionFab?.getCoordinates()?.let { coordinates ->
                binding.revealView.hideRevealEffect(coordinates.x, coordinates.y, 1920)
            }
        }

        super.onActivityResumed(activity)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.manga, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        // Hide options for local manga
        menu.findItem(R.id.action_share).isVisible = !isLocalSource
        menu.findItem(R.id.download_group).isVisible = !isLocalSource

        // Hide options for non-library manga
        menu.findItem(R.id.action_edit_categories).isVisible = presenter.manga.favorite && presenter.getCategories().isNotEmpty()
        menu.findItem(R.id.action_edit_cover).isVisible = presenter.manga.favorite
        menu.findItem(R.id.action_migrate).isVisible = presenter.manga.favorite
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> shareManga()
            R.id.download_next, R.id.download_next_5, R.id.download_next_10,
            R.id.download_custom, R.id.download_unread, R.id.download_all
            -> downloadChapters(item.itemId)

            R.id.action_share_cover -> shareCover()
            R.id.action_save_cover -> saveCover()
            R.id.action_edit_cover -> changeCover()

            R.id.action_edit_categories -> onCategoriesClick()
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
            mangaInfoAdapter?.update(manga, source)
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

    fun onTrackingCount(trackCount: Int) {
        mangaInfoAdapter?.setTrackingCount(trackCount)
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
            activity?.invalidateOptionsMenu()
        } else {
            addToLibrary(manga)
        }
    }

    fun onTrackingClick() {
        trackSheet?.show()
    }

    private fun addToLibrary(manga: Manga) {
        val categories = presenter.getCategories()
        val defaultCategoryId = preferences.defaultCategory()
        val defaultCategory = categories.find { it.id == defaultCategoryId }

        when {
            // Default category set
            defaultCategory != null -> {
                toggleFavorite()
                presenter.moveMangaToCategory(manga, defaultCategory)
                activity?.toast(activity?.getString(R.string.manga_added_library))
                activity?.invalidateOptionsMenu()
            }

            // Automatic 'Default' or no categories
            defaultCategoryId == 0 || categories.isEmpty() -> {
                toggleFavorite()
                presenter.moveMangaToCategory(manga, null)
                activity?.toast(activity?.getString(R.string.manga_added_library))
                activity?.invalidateOptionsMenu()
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

        if (source != null && preferences.autoAddTrack()) {
            presenter.trackList
                .map { it.service }
                .filterIsInstance<UnattendedTrackService>()
                .filter { it.accept(source!!) }
                .forEach { service ->
                    launchIO {
                        try {
                            service.match(manga)?.let { track ->
                                presenter.registerTracking(track, service as TrackService)
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "Could not match manga: ${manga.title} with service $service")
                        }
                    }
                }
        }
    }

    /**
     * Toggles the favorite status and asks for confirmation to delete downloaded chapters.
     */
    private fun toggleFavorite() {
        val isNowFavorite = presenter.toggleFavorite()
        if (activity != null && !isNowFavorite && presenter.hasDownloads()) {
            (activity as? MainActivity)?.binding?.rootCoordinator?.snack(activity!!.getString(R.string.delete_downloads_for_manga)) {
                setAction(R.string.action_delete) {
                    presenter.deleteDownloads()
                }
            }
        }

        mangaInfoAdapter?.notifyDataSetChanged()
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
            activity?.invalidateOptionsMenu()
        }

        presenter.moveMangaToCategories(manga, categories)
    }

    /**
     * Perform a global search using the provided query.
     *
     * @param query the search query to pass to the search controller
     */
    fun performGlobalSearch(query: String) {
        router.pushController(GlobalSearchController(query).withFadeTransaction())
    }

    /**
     * Perform a search using the provided query.
     *
     * @param query the search query to the parent controller
     */
    fun performSearch(query: String) {
        if (router.backstackSize < 2) {
            return
        }

        when (val previousController = router.backstack[router.backstackSize - 2].controller) {
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
            is LatestUpdatesController -> {
                // Search doesn't currently work in source Latest view
                return
            }
            is BrowseSourceController -> {
                router.handleBack()
                previousController.searchWithQuery(query)
            }
        }
    }

    /**
     * Performs a genre search using the provided genre name.
     *
     * @param genreName the search genre to the parent controller
     */
    fun performGenreSearch(genreName: String) {
        if (router.backstackSize < 2) {
            return
        }

        val previousController = router.backstack[router.backstackSize - 2].controller
        val presenterSource = presenter.source

        if (previousController is BrowseSourceController &&
            presenterSource is HttpSource
        ) {
            router.handleBack()
            previousController.searchWithGenre(genreName)
        } else {
            performSearch(genreName)
        }
    }

    fun shareCover() {
        try {
            val activity = activity!!
            val cover = presenter.shareCover(activity)
            val uri = cover.getUriCompat(activity)
            startActivity(Intent.createChooser(uri.toShareIntent(), activity.getString(R.string.action_share)))
        } catch (e: Exception) {
            Timber.e(e)
            activity?.toast(R.string.error_sharing_cover)
        }
    }

    fun saveCover() {
        try {
            presenter.saveCover(activity!!)
            activity?.toast(R.string.cover_saved)
        } catch (e: Exception) {
            Timber.e(e)
            activity?.toast(R.string.error_saving_cover)
        }
    }

    fun changeCover() {
        val manga = manga ?: return
        if (manga.hasCustomCover(coverCache)) {
            ChangeMangaCoverDialog(this, manga).showDialog(router)
        } else {
            openMangaCoverPicker(manga)
        }
    }

    override fun openMangaCoverPicker(manga: Manga) {
        if (manga.favorite) {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(
                Intent.createChooser(
                    intent,
                    resources?.getString(R.string.file_select_cover)
                ),
                REQUEST_IMAGE_OPEN
            )
        } else {
            activity?.toast(R.string.notification_first_add_to_library)
        }

        destroyActionModeIfNeeded()
    }

    override fun deleteMangaCover(manga: Manga) {
        presenter.deleteCustomCover(manga)
        mangaInfoAdapter?.notifyDataSetChanged()
        destroyActionModeIfNeeded()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_OPEN) {
            val dataUri = data?.data
            if (dataUri == null || resultCode != Activity.RESULT_OK) return
            val activity = activity ?: return
            presenter.editCover(manga!!, activity, dataUri)
        }
    }

    fun onSetCoverSuccess() {
        mangaInfoAdapter?.notifyDataSetChanged()
        activity?.toast(R.string.cover_updated)
    }

    fun onSetCoverError(error: Throwable) {
        activity?.toast(R.string.notification_cover_update_failed)
        Timber.e(error)
    }

    /**
     * Initiates source migration for the specific manga.
     */
    private fun migrateManga() {
        val controller = SearchController(presenter.manga)
        controller.targetController = this
        router.pushController(controller.withFadeTransaction())
    }

    // Manga info - end

    // Chapters list - start

    fun onNextChapters(chapters: List<ChapterItem>) {
        // If the list is empty and it hasn't requested previously, fetch chapters from source
        // We use presenter chapters instead because they are always unfiltered
        if (!presenter.hasRequested && presenter.allChapters.isEmpty()) {
            fetchChaptersFromSource()
        }

        val chaptersHeader = chaptersHeaderAdapter ?: return
        chaptersHeader.setNumChapters(chapters.size)

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
        if (context != null) {
            actionFab?.isVisible = chapters.any { !it.read }
            if (chapters.any { it.read }) {
                actionFab?.text = context.getString(R.string.action_resume)
            }
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
        if (error is NoChaptersException) {
            activity?.toast(activity?.getString(R.string.no_chapters_error))
        } else {
            activity?.toast(error.message)
        }
    }

    fun onChapterDownloadUpdate(download: Download) {
        chaptersAdapter?.currentItems?.find { it.id == download.chapter.id }?.let {
            chaptersAdapter?.updateItem(it, it.status)
        }
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
            if (adapter.isSelected(position)) {
                lastClickPositionStack.remove(position) // possible that it's not there, but no harm
            } else {
                lastClickPositionStack.push(position)
            }

            toggleSelection(position)
            true
        } else {
            openChapter(item.chapter)
            false
        }
    }

    override fun onItemLongClick(position: Int) {
        createActionModeIfNeeded()
        val lastClickPosition = lastClickPositionStack.peek()!!
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
        if (lastClickPosition != position) {
            lastClickPositionStack.remove(position) // move to top if already exists
            lastClickPositionStack.push(position)
        }
        chaptersAdapter?.notifyDataSetChanged()
    }

    fun showSettingsSheet() {
        settingsSheet?.show()
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
        lastClickPositionStack.clear()
        lastClickPositionStack.push(-1)
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

            val chapters = getSelectedChapters()
            binding.actionToolbar.findItem(R.id.action_download)?.isVisible = !isLocalSource && chapters.any { !it.isDownloaded }
            binding.actionToolbar.findItem(R.id.action_delete)?.isVisible = !isLocalSource && chapters.any { it.isDownloaded }
            binding.actionToolbar.findItem(R.id.action_bookmark)?.isVisible = chapters.any { !it.chapter.bookmark }
            binding.actionToolbar.findItem(R.id.action_remove_bookmark)?.isVisible = chapters.all { it.chapter.bookmark }
            binding.actionToolbar.findItem(R.id.action_mark_as_read)?.isVisible = chapters.any { !it.chapter.read }
            binding.actionToolbar.findItem(R.id.action_mark_as_unread)?.isVisible = chapters.all { it.chapter.read }

            // Hide FAB to avoid interfering with the bottom action toolbar
            actionFab?.isVisible = false
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
        actionFab?.isVisible = true
    }

    override fun onDetach(view: View) {
        destroyActionModeIfNeeded()
        super.onDetach(view)
    }

    override fun downloadChapter(position: Int) {
        val item = chaptersAdapter?.getItem(position) ?: return
        if (item.status == Download.State.ERROR) {
            DownloadService.start(activity!!)
        } else {
            downloadChapters(listOf(item))
        }
        chaptersAdapter?.updateItem(item)
    }

    override fun deleteChapter(position: Int) {
        val item = chaptersAdapter?.getItem(position) ?: return
        deleteChapters(listOf(item))
        chaptersAdapter?.updateItem(item)
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
        destroyActionModeIfNeeded()
    }

    private fun markAsUnread(chapters: List<ChapterItem>) {
        presenter.markChaptersRead(chapters, false)
        destroyActionModeIfNeeded()
    }

    private fun downloadChapters(chapters: List<ChapterItem>) {
        if (source is SourceManager.StubSource) {
            activity?.toast(R.string.loader_not_implemented_error)
            return
        }

        val view = view
        val manga = presenter.manga
        presenter.downloadChapters(chapters)
        if (view != null && !manga.favorite) {
            addSnackbar = (activity as? MainActivity)?.binding?.rootCoordinator?.snack(view.context.getString(R.string.snack_add_to_library), Snackbar.LENGTH_INDEFINITE) {
                setAction(R.string.action_add) {
                    if (!manga.favorite) {
                        addToLibrary(manga)
                    }
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
        val chapterPos = prevChapters.indexOf(chapters.lastOrNull())
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
        launchUI {
            chaptersAdapter?.notifyDataSetChanged()
        }
    }

    fun onChaptersDeletedError(error: Throwable) {
        Timber.e(error)
    }

    override fun startDownloadNow(position: Int) {
        val chapter = chaptersAdapter?.getItem(position) ?: return
        presenter.startDownloadingNow(chapter)
    }

    // OVERFLOW MENU DIALOGS

    private fun downloadChapters(choice: Int) {
        val chaptersToDownload = when (choice) {
            R.id.download_next -> presenter.getUnreadChaptersSorted().take(1)
            R.id.download_next_5 -> presenter.getUnreadChaptersSorted().take(5)
            R.id.download_next_10 -> presenter.getUnreadChaptersSorted().take(10)
            R.id.download_custom -> {
                showCustomDownloadDialog()
                return
            }
            R.id.download_unread -> presenter.allChapters.filter { !it.read }
            R.id.download_all -> presenter.allChapters
            else -> emptyList()
        }
        if (chaptersToDownload.isNotEmpty()) {
            downloadChapters(chaptersToDownload)
        }
        destroyActionModeIfNeeded()
    }

    private fun showCustomDownloadDialog() {
        DownloadCustomChaptersDialog(
            this,
            presenter.allChapters.size
        ).showDialog(router)
    }

    override fun downloadCustomChapters(amount: Int) {
        val chaptersToDownload = presenter.getUnreadChaptersSorted().take(amount)
        if (chaptersToDownload.isNotEmpty()) {
            downloadChapters(chaptersToDownload)
        }
    }

    // Chapters list - end

    // Tracker sheet - start
    fun onNextTrackers(trackers: List<TrackItem>) {
        trackSheet?.onNextTrackers(trackers)
    }

    fun onTrackingRefreshDone() {
    }

    fun onTrackingRefreshError(error: Throwable) {
        Timber.e(error)
        activity?.toast(error.message)
    }

    fun onTrackingSearchResults(results: List<TrackSearch>) {
        getTrackingSearchDialog()?.onSearchResults(results)
    }

    fun onTrackingSearchResultsError(error: Throwable) {
        Timber.e(error)
        getTrackingSearchDialog()?.onSearchResultsError(error.message)
    }

    private fun getTrackingSearchDialog(): TrackSearchDialog? {
        return trackSheet?.getSearchDialog()
    }

    // Tracker sheet - end

    private val chapterRecycler: RecyclerView
        get() = binding.fullRecycler ?: binding.chaptersRecycler!!

    companion object {
        const val FROM_SOURCE_EXTRA = "from_source"
        const val MANGA_EXTRA = "manga"

        /**
         * Key to change the cover of a manga in [onActivityResult].
         */
        const val REQUEST_IMAGE_OPEN = 101
    }
}
