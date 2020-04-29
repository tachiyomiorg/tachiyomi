package eu.kanade.tachiyomi.ui.library

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.drawable.DrawableCompat
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.f2prateek.rx.preferences.Preference
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.LibraryControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.toast
import java.io.IOException
import kotlinx.android.synthetic.main.main_activity.tabs
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.queryTextChanges
import reactivecircus.flowbinding.viewpager.pageSelections
import rx.Subscription
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryController(
    bundle: Bundle? = null,
    private val preferences: PreferencesHelper = Injekt.get()
) : NucleusController<LibraryControllerBinding, LibraryPresenter>(bundle),
    RootController,
    TabbedController,
    ActionMode.Callback,
    ChangeMangaCategoriesDialog.Listener,
    DeleteLibraryMangasDialog.Listener {

    /**
     * Position of the active category.
     */
    private var activeCategory: Int = preferences.lastUsedCategory().get()

    /**
     * Action mode for selections.
     */
    private var actionMode: ActionMode? = null

    /**
     * Library search query.
     */
    private var query = ""

    /**
     * Currently selected mangas.
     */
    val selectedMangas = mutableSetOf<Manga>()

    private var selectedCoverManga: Manga? = null

    /**
     * Relay to notify the UI of selection updates.
     */
    val selectionRelay: PublishRelay<LibrarySelectionEvent> = PublishRelay.create()

    /**
     * Relay to notify search query changes.
     */
    val searchRelay: BehaviorRelay<String> = BehaviorRelay.create()

    /**
     * Relay to notify the library's viewpager for updates.
     */
    val libraryMangaRelay: BehaviorRelay<LibraryMangaEvent> = BehaviorRelay.create()

    /**
     * Relay to notify the library's viewpager to select all manga
     */
    val selectAllRelay: PublishRelay<Int> = PublishRelay.create()

    /**
     * Relay to notify the library's viewpager to select the inverse
     */
    val selectInverseRelay: PublishRelay<Int> = PublishRelay.create()

    /**
     * Number of manga per row in grid mode.
     */
    var mangaPerRow = 0
        private set

    /**
     * Adapter of the view pager.
     */
    private var adapter: LibraryAdapter? = null

    /**
     * Sheet containing filter/sort/display items.
     */
    private var settingsSheet: LibrarySettingsSheet? = null

    private var tabsVisibilityRelay: BehaviorRelay<Boolean> = BehaviorRelay.create(false)

    private var tabsVisibilitySubscription: Subscription? = null

    init {
        setHasOptionsMenu(true)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_library)
    }

    override fun createPresenter(): LibraryPresenter {
        return LibraryPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = LibraryControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = LibraryAdapter(this)
        binding.libraryPager.adapter = adapter
        binding.libraryPager.pageSelections()
            .onEach {
                preferences.lastUsedCategory().set(it)
                activeCategory = it
            }
            .launchIn(scope)

        getColumnsPreferenceForCurrentOrientation().asObservable()
            .doOnNext { mangaPerRow = it }
            .skip(1)
            // Set again the adapter to recalculate the covers height
            .subscribeUntilDestroy { reattachAdapter() }

        if (selectedMangas.isNotEmpty()) {
            createActionModeIfNeeded()
        }

        settingsSheet = LibrarySettingsSheet(activity!!) { group ->
            when (group) {
                is LibrarySettingsSheet.Filter.FilterGroup -> onFilterChanged()
                is LibrarySettingsSheet.Sort.SortGroup -> onSortChanged()
                is LibrarySettingsSheet.Display.DisplayGroup -> reattachAdapter()
                is LibrarySettingsSheet.Display.BadgeGroup -> onDownloadBadgeChanged()
            }
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            activity?.tabs?.setupWithViewPager(binding.libraryPager)
            presenter.subscribeLibrary()
        }
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        binding.actionToolbar.destroy()
        adapter?.onDestroy()
        adapter = null
        settingsSheet = null
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = null
        super.onDestroyView(view)
    }

    override fun configureTabs(tabs: TabLayout) {
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_CENTER
            tabMode = TabLayout.MODE_SCROLLABLE
        }
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = tabsVisibilityRelay.subscribe { visible ->
            val tabAnimator = (activity as? MainActivity)?.tabAnimator
            if (visible) {
                tabAnimator?.expand()
            } else {
                tabAnimator?.collapse()
            }
        }
    }

    override fun cleanupTabs(tabs: TabLayout) {
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = null
    }

    fun showSettingsSheet() {
        settingsSheet?.show()
    }

    fun onNextLibraryUpdate(categories: List<Category>, mangaMap: Map<Int, List<LibraryItem>>) {
        val view = view ?: return
        val adapter = adapter ?: return

        // Show empty view if needed
        if (mangaMap.isNotEmpty()) {
            binding.emptyView.hide()
        } else {
            binding.emptyView.show(R.string.information_empty_library)
        }

        // Get the current active category.
        val activeCat = if (adapter.categories.isNotEmpty()) {
            binding.libraryPager.currentItem
        } else {
            activeCategory
        }

        // Set the categories
        adapter.categories = categories

        // Restore active category.
        binding.libraryPager.setCurrentItem(activeCat, false)

        tabsVisibilityRelay.call(categories.size > 1)

        // Delay the scroll position to allow the view to be properly measured.
        view.post {
            if (isAttached) {
                activity?.tabs?.setScrollPosition(binding.libraryPager.currentItem, 0f, true)
            }
        }

        // Send the manga map to child fragments after the adapter is updated.
        libraryMangaRelay.call(LibraryMangaEvent(mangaMap))
    }

    /**
     * Returns a preference for the number of manga per row based on the current orientation.
     *
     * @return the preference.
     */
    private fun getColumnsPreferenceForCurrentOrientation(): Preference<Int> {
        return if (resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT) {
            preferences.portraitColumns()
        } else {
            preferences.landscapeColumns()
        }
    }

    /**
     * Called when a filter is changed.
     */
    private fun onFilterChanged() {
        presenter.requestFilterUpdate()
        activity?.invalidateOptionsMenu()
    }

    private fun onDownloadBadgeChanged() {
        presenter.requestDownloadBadgesUpdate()
    }

    /**
     * Called when the sorting mode is changed.
     */
    private fun onSortChanged() {
        presenter.requestSortUpdate()
    }

    /**
     * Reattaches the adapter to the view pager to recreate fragments
     */
    private fun reattachAdapter() {
        val adapter = adapter ?: return

        val position = binding.libraryPager.currentItem

        adapter.recycle = false
        binding.libraryPager.adapter = adapter
        binding.libraryPager.currentItem = position
        adapter.recycle = true
    }

    /**
     * Creates the action mode if it's not created already.
     */
    fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)
            binding.actionToolbar.show(
                actionMode!!,
                R.menu.library_selection
            ) { onActionItemClicked(actionMode!!, it!!) }
        }
    }

    /**
     * Destroys the action mode.
     */
    private fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE

        searchView.queryTextChanges()
            // Ignore events if this controller isn't at the top
            .filter { router.backstack.lastOrNull()?.controller() == this }
            .onEach {
                query = it.toString()
                searchRelay.call(query)
            }
            .launchIn(scope)

        if (query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()

            // Manually trigger the search since the binding doesn't trigger for some reason
            searchRelay.call(query)
        }

        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })

        // Mutate the filter icon because it needs to be tinted and the resource is shared.
        menu.findItem(R.id.action_filter).icon.mutate()
    }

    fun search(query: String) {
        this.query = query
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val settingsSheet = settingsSheet ?: return

        val filterItem = menu.findItem(R.id.action_filter)

        // Tint icon if there's a filter active
        if (settingsSheet.filters.hasActiveFilters()) {
            val filterColor = activity!!.getResourceColor(R.attr.colorFilterActive)
            DrawableCompat.setTint(filterItem.icon, filterColor)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_filter -> showSettingsSheet()
            R.id.action_update_library -> {
                activity?.let {
                    if (LibraryUpdateService.start(it)) {
                        it.toast(R.string.updating_library)
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Invalidates the action mode, forcing it to refresh its content.
     */
    fun invalidateActionMode() {
        actionMode?.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.generic_selection, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = selectedMangas.size
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = count.toString()

            binding.actionToolbar.findItem(R.id.action_edit_cover)?.isVisible = count == 1
        }
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_edit_cover -> {
                changeSelectedCover()
                destroyActionModeIfNeeded()
            }
            R.id.action_move_to_category -> showChangeMangaCategoriesDialog()
            R.id.action_delete -> showDeleteMangaDialog()
            R.id.action_select_all -> selectAllCategoryManga()
            R.id.action_select_inverse -> selectInverseCategoryManga()
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        binding.actionToolbar.hide()
        // Clear all the manga selections and notify child views.
        selectedMangas.clear()
        selectionRelay.call(LibrarySelectionEvent.Cleared())
        actionMode = null
    }

    fun openManga(manga: Manga) {
        // Notify the presenter a manga is being opened.
        presenter.onOpenManga()

        router.pushController(MangaController(manga).withFadeTransaction())
    }

    /**
     * Sets the selection for a given manga.
     *
     * @param manga the manga whose selection has changed.
     * @param selected whether it's now selected or not.
     */
    fun setSelection(manga: Manga, selected: Boolean) {
        if (selected) {
            if (selectedMangas.add(manga)) {
                selectionRelay.call(LibrarySelectionEvent.Selected(manga))
            }
        } else {
            if (selectedMangas.remove(manga)) {
                selectionRelay.call(LibrarySelectionEvent.Unselected(manga))
            }
        }
    }

    /**
     * Toggles the current selection state for a given manga.
     *
     * @param manga the manga whose selection to change.
     */
    fun toggleSelection(manga: Manga) {
        if (selectedMangas.add(manga)) {
            selectionRelay.call(LibrarySelectionEvent.Selected(manga))
        } else if (selectedMangas.remove(manga)) {
            selectionRelay.call(LibrarySelectionEvent.Unselected(manga))
        }
    }

    /**
     * Move the selected manga to a list of categories.
     */
    private fun showChangeMangaCategoriesDialog() {
        // Create a copy of selected manga
        val mangas = selectedMangas.toList()

        // Hide the default category because it has a different behavior than the ones from db.
        val categories = presenter.categories.filter { it.id != 0 }

        // Get indexes of the common categories to preselect.
        val commonCategoriesIndexes = presenter.getCommonCategories(mangas)
            .map { categories.indexOf(it) }
            .toTypedArray()

        ChangeMangaCategoriesDialog(this, mangas, categories, commonCategoriesIndexes)
            .showDialog(router)
    }

    private fun showDeleteMangaDialog() {
        DeleteLibraryMangasDialog(this, selectedMangas.toList()).showDialog(router)
    }

    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        presenter.moveMangasToCategories(categories, mangas)
        destroyActionModeIfNeeded()
    }

    override fun deleteMangasFromLibrary(mangas: List<Manga>, deleteChapters: Boolean) {
        presenter.removeMangaFromLibrary(mangas, deleteChapters)
        destroyActionModeIfNeeded()
    }

    /**
     * Changes the cover for the selected manga.
     */
    private fun changeSelectedCover() {
        val manga = selectedMangas.firstOrNull() ?: return
        selectedCoverManga = manga

        if (manga.favorite) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
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
    }

    private fun selectAllCategoryManga() {
        adapter?.categories?.getOrNull(binding.libraryPager.currentItem)?.id?.let {
            selectAllRelay.call(it)
        }
    }

    private fun selectInverseCategoryManga() {
        adapter?.categories?.getOrNull(binding.libraryPager.currentItem)?.id?.let {
            selectInverseRelay.call(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_OPEN) {
            if (data == null || resultCode != Activity.RESULT_OK) return
            val activity = activity ?: return
            val manga = selectedCoverManga ?: return

            try {
                // Get the file's input stream from the incoming Intent
                activity.contentResolver.openInputStream(data.data ?: Uri.EMPTY).use {
                    // Update cover to selected file, show error if something went wrong
                    if (it != null && presenter.editCoverWithStream(it, manga)) {
                        // TODO refresh cover
                    } else {
                        activity.toast(R.string.notification_cover_update_failed)
                    }
                }
            } catch (error: IOException) {
                activity.toast(R.string.notification_cover_update_failed)
                Timber.e(error)
            }
            selectedCoverManga = null
        }
    }

    private companion object {
        /**
         * Key to change the cover of a manga in [onActivityResult].
         */
        const val REQUEST_IMAGE_OPEN = 101
    }
}
