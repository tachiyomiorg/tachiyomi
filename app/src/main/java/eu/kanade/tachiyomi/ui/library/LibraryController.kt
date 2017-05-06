package eu.kanade.tachiyomi.ui.library

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.SearchView
import android.view.*
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.f2prateek.rx.preferences.Preference
import com.jakewharton.rxbinding.support.v4.view.pageSelections
import com.jakewharton.rxbinding.support.v7.widget.queryTextChanges
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.SecondaryDrawerController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.DrawerSwipeCloseListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.library_controller.view.*
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException


class LibraryController(
        bundle: Bundle? = null,
        private val preferences: PreferencesHelper = Injekt.get()
) : NucleusController<LibraryPresenter>(bundle),
        TabbedController,
        SecondaryDrawerController,
        ActionMode.Callback,
        ChangeMangaCategoriesDialog.Listener,
        DeleteLibraryMangasDialog.Listener {

    /**
     * Position of the active category.
     */
    var activeCategory: Int = preferences.lastUsedCategory().getOrDefault()
        private set

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
    val selectedMangas = mutableListOf<Manga>()

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
     * Number of manga per row in grid mode.
     */
    var mangaPerRow = 0
        private set

    /**
     * TabLayout of the categories.
     */
    private val tabs: TabLayout?
        get() = activity?.tabs

    private val drawer: DrawerLayout?
        get() = activity?.drawer

    private var adapter: LibraryAdapter? = null

    /**
     * Navigation view containing filter/sort/display items.
     */
    private var navView: LibraryNavigationView? = null

    /**
     * Drawer listener to allow swipe only for closing the drawer.
     */
    private var drawerListener: DrawerLayout.DrawerListener? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_library)
    }

    override fun createPresenter(): LibraryPresenter {
        return LibraryPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.library_controller, container, false)
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        adapter = LibraryAdapter(this)
        with(view) {
            view_pager.adapter = adapter
            view_pager.pageSelections().skip(1).subscribeUntilDestroy {
                preferences.lastUsedCategory().set(it)
                activeCategory = it
            }

            getColumnsPreferenceForCurrentOrientation().asObservable()
                    .doOnNext { mangaPerRow = it }
                    .skip(1)
                    // Set again the adapter to recalculate the covers height
                    .subscribeUntilDestroy { reattachAdapter() }

            if (selectedMangas.isNotEmpty()) {
                createActionModeIfNeeded()
            }
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            activity?.tabs?.setupWithViewPager(view?.view_pager)
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.subscribeLibrary()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
        actionMode = null
    }

    override fun createSecondaryDrawer(drawer: DrawerLayout): ViewGroup {
        val view = drawer.inflate(R.layout.library_drawer) as LibraryNavigationView
        drawerListener = DrawerSwipeCloseListener(drawer, view).also {
            drawer.addDrawerListener(it)
        }
        navView = view

        navView?.post {
            if (isAttached && drawer.isDrawerOpen(navView))
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navView)
        }

        navView?.onGroupClicked = { group ->
            when (group) {
                is LibraryNavigationView.FilterGroup -> onFilterChanged()
                is LibraryNavigationView.SortGroup -> onSortChanged()
                is LibraryNavigationView.DisplayGroup -> reattachAdapter()
            }
        }

        return view
    }

    override fun cleanupSecondaryDrawer(drawer: DrawerLayout) {
        drawerListener?.let { drawer.removeDrawerListener(it) }
        drawerListener = null
        navView = null
    }

    fun onNextLibraryUpdate(categories: List<Category>, mangaMap: Map<Int, List<LibraryItem>>) {
        val view = view ?: return
        val adapter = adapter ?: return

        // Show empty view if needed
        if (mangaMap.isNotEmpty()) {
            view.empty_view.hide()
        } else {
            view.empty_view.show(R.drawable.ic_book_black_128dp, R.string.information_empty_library)
        }

        // Get the current active category.
        val activeCat = if (adapter.categories.isNotEmpty())
            view.view_pager.currentItem
        else
            activeCategory

        // Set the categories
        adapter.categories = categories

        // Restore active category.
        view.view_pager.setCurrentItem(activeCat, false)

        tabs?.visibility = if (categories.size <= 1) View.GONE else View.VISIBLE

        // Delay the scroll position to allow the view to be properly measured.
        view.post {
            if (isAttached) {
                tabs?.setScrollPosition(view.view_pager.currentItem, 0f, true)
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
        return if (resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT)
            preferences.portraitColumns()
        else
            preferences.landscapeColumns()
    }

    /**
     * Called when a filter is changed.
     */
    private fun onFilterChanged() {
        presenter.requestFilterUpdate()
        (activity as? AppCompatActivity)?.supportInvalidateOptionsMenu()
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
        val pager = view?.view_pager ?: return
        val adapter = adapter ?: return

        val position = pager.currentItem

        adapter.recycle = false
        pager.adapter = adapter
        pager.currentItem = position
        adapter.recycle = true
    }

    override fun configureTabs(tabs: TabLayout) {
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_CENTER
            tabMode = TabLayout.MODE_SCROLLABLE
        }
    }

    /**
     * Creates the action mode if it's not created already.
     */
    fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)
        }
    }

    /**
     * Destroys the action mode.
     */
    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        if (!query.isNullOrEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }

        // Mutate the filter icon because it needs to be tinted and the resource is shared.
        menu.findItem(R.id.action_filter).icon.mutate()

        searchView.queryTextChanges().subscribeUntilDestroy {
            query = it.toString()
            searchRelay.call(query)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val navView = navView ?: return

        val filterItem = menu.findItem(R.id.action_filter)

        // Tint icon if there's a filter active
        val filterColor = if (navView.hasActiveFilters()) Color.rgb(255, 238, 7) else Color.WHITE
        DrawableCompat.setTint(filterItem.icon, filterColor)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> {
                navView?.let { drawer?.openDrawer(Gravity.END) }
            }
            R.id.action_update_library -> {
                activity?.let { LibraryUpdateService.start(it) }
            }
            R.id.action_edit_categories -> {
                router.pushController(RouterTransaction.with(CategoryController())
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler()))
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    /**
     * Invalidates the action mode, forcing it to refresh its content.
     */
    fun invalidateActionMode() {
        actionMode?.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.library_selection, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = selectedMangas.size
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
            R.id.action_edit_cover -> {
                changeSelectedCover()
                destroyActionModeIfNeeded()
            }
            R.id.action_move_to_category -> showChangeMangaCategoriesDialog()
            R.id.action_delete -> showDeleteMangaDialog()
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        // Clear all the manga selections and notify child views.
        selectedMangas.clear()
        selectionRelay.call(LibrarySelectionEvent.Cleared())
        actionMode = null
    }

    fun openManga(manga: Manga) {
        // Notify the presenter a manga is being opened.
        presenter.onOpenManga()

        router.pushController(RouterTransaction.with(MangaController(manga))
                .pushChangeHandler(FadeChangeHandler())
                .popChangeHandler(FadeChangeHandler()))
    }

    /**
     * Sets the selection for a given manga.
     *
     * @param manga the manga whose selection has changed.
     * @param selected whether it's now selected or not.
     */
    fun setSelection(manga: Manga, selected: Boolean) {
        if (selected) {
            selectedMangas.add(manga)
            selectionRelay.call(LibrarySelectionEvent.Selected(manga))
        } else {
            selectedMangas.remove(manga)
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
                .showDialog(router, null)
    }

    private fun showDeleteMangaDialog() {
        DeleteLibraryMangasDialog(this, selectedMangas.toList()).showDialog(router, null)
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
     *
     * @param mangas a list of selected manga.
     */
    private fun changeSelectedCover() {
        val manga = selectedMangas.firstOrNull() ?: return
        selectedCoverManga = manga

        if (manga.favorite) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent,
                    resources?.getString(R.string.file_select_cover)), REQUEST_IMAGE_OPEN)
        } else {
            activity?.toast(R.string.notification_first_add_to_library)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_OPEN) {
            if (data == null || resultCode != Activity.RESULT_OK) return
            val activity = activity ?: return
            val manga = selectedCoverManga ?: return

            try {
                // Get the file's input stream from the incoming Intent
                activity.contentResolver.openInputStream(data.data).use {
                    // Update cover to selected file, show error if something went wrong
                    if (presenter.editCoverWithStream(it, manga)) {
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