package eu.kanade.tachiyomi.ui.library

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.view.ActionMode
import android.support.v7.widget.SearchView
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import com.f2prateek.rx.preferences.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.category.CategoryActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_library.*
import nucleus.factory.RequiresPresenter
import rx.Subscription
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.IOException

/**
 * Fragment that shows the manga from the library.
 * Uses R.layout.fragment_library.
 */
@RequiresPresenter(LibraryPresenter::class)
class LibraryFragment : BaseRxFragment<LibraryPresenter>(), ActionMode.Callback {

    /**
     * Adapter containing the categories of the library.
     */
    lateinit var adapter: LibraryAdapter
        private set

    /**
     * Preferences.
     */
    val preferences: PreferencesHelper by injectLazy()

    /**
     * TabLayout of the categories.
     */
    private val tabs: TabLayout
        get() = (activity as MainActivity).tabs

    /**
     * Position of the active category.
     */
    private var activeCategory: Int = 0

    /**
     * Query of the search box.
     */
    private var query: String? = null

    /**
     * Action mode for manga selection.
     */
    private var actionMode: ActionMode? = null

    /**
     * Selected manga for editing its cover.
     */
    private var selectedCoverManga: Manga? = null

    /**
     * Number of manga per row in grid mode.
     */
    var mangaPerRow = 0
        private set

    /**
     * Navigation view containing filter/sort/display items.
     */
    private lateinit var navView: LibraryNavigationView

    /**
     * Drawer listener to allow swipe only for closing the drawer.
     */
    private val drawerListener by lazy {
        object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                if (drawerView == navView) {
                    activity.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navView)
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                if (drawerView == navView) {
                    activity.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, navView)
                }
            }
        }
    }

    /**
     * Subscription for the number of manga per row.
     */
    private var numColumnsSubscription: Subscription? = null

    companion object {
        /**
         * Key to change the cover of a manga in [onActivityResult].
         */
        const val REQUEST_IMAGE_OPEN = 101

        /**
         * Key to save and restore [query] from a [Bundle].
         */
        const val QUERY_KEY = "query_key"

        /**
         * Key to save and restore [activeCategory] from a [Bundle].
         */
        const val CATEGORY_KEY = "category_key"

        /**
         * Creates a new instance of this fragment.
         *
         * @return a new instance of [LibraryFragment].
         */
        fun newInstance(): LibraryFragment {
            return LibraryFragment()
        }
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        setToolbarTitle(getString(R.string.label_library))

        adapter = LibraryAdapter(this)
        view_pager.adapter = adapter
        view_pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                preferences.lastUsedCategory().set(position)
            }
        })
        tabs.setupWithViewPager(view_pager)

        if (savedState != null) {
            activeCategory = savedState.getInt(CATEGORY_KEY)
            query = savedState.getString(QUERY_KEY)
            presenter.searchSubject.call(query)
            if (presenter.selectedMangas.isNotEmpty()) {
                createActionModeIfNeeded()
            }
        } else {
            activeCategory = preferences.lastUsedCategory().getOrDefault()
        }

        numColumnsSubscription = getColumnsPreferenceForCurrentOrientation().asObservable()
                .doOnNext { mangaPerRow = it }
                .skip(1)
                // Set again the adapter to recalculate the covers height
                .subscribe { reattachAdapter() }


        // Inflate and prepare drawer
        navView = activity.drawer.inflate(R.layout.library_drawer) as LibraryNavigationView
        activity.drawer.addView(navView)
        activity.drawer.addDrawerListener(drawerListener)

        navView.post {
            if (isAdded && !activity.drawer.isDrawerOpen(navView))
                activity.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navView)
        }

        navView.onGroupClicked = { group ->
            when (group) {
                is LibraryNavigationView.FilterGroup -> onFilterChanged()
                is LibraryNavigationView.SortGroup -> onSortChanged()
                is LibraryNavigationView.DisplayGroup -> reattachAdapter()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.subscribeLibrary()
    }

    override fun onDestroyView() {
        activity.drawer.removeDrawerListener(drawerListener)
        activity.drawer.removeView(navView)
        numColumnsSubscription?.unsubscribe()
        tabs.setupWithViewPager(null)
        tabs.visibility = View.GONE
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CATEGORY_KEY, view_pager.currentItem)
        outState.putString(QUERY_KEY, query)
        super.onSaveInstanceState(outState)
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

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                onSearchTextChange(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                onSearchTextChange(newText)
                return true
            }
        })

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val filterItem = menu.findItem(R.id.action_filter)

        // Tint icon if there's a filter active
        val filterColor = if (navView.hasActiveFilters()) Color.rgb(255, 238, 7) else Color.WHITE
        DrawableCompat.setTint(filterItem.icon, filterColor)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> {
                activity.drawer.openDrawer(Gravity.END)
            }
            R.id.action_update_library -> {
                LibraryUpdateService.start(activity)
            }
            R.id.action_edit_categories -> {
                val intent = CategoryActivity.newIntent(activity)
                startActivity(intent)
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    /**
     * Called when a filter is changed.
     */
    private fun onFilterChanged() {
        presenter.requestLibraryUpdate()
        activity.supportInvalidateOptionsMenu()
    }

    /**
     * Called when the sorting mode is changed.
     */
    private fun onSortChanged() {
        presenter.requestLibraryUpdate()
    }

    /**
     * Reattaches the adapter to the view pager to recreate fragments
     */
    private fun reattachAdapter() {
        val position = view_pager.currentItem
        adapter.recycle = false
        view_pager.adapter = adapter
        view_pager.currentItem = position
        adapter.recycle = true
    }

    /**
     * Returns a preference for the number of manga per row based on the current orientation.
     *
     * @return the preference.
     */
    private fun getColumnsPreferenceForCurrentOrientation(): Preference<Int> {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            preferences.portraitColumns()
        else
            preferences.landscapeColumns()
    }

    /**
     * Updates the query.
     *
     * @param query the new value of the query.
     */
    private fun onSearchTextChange(query: String?) {
        this.query = query

        // Notify the subject the query has changed.
        if (isResumed) {
            presenter.searchSubject.call(query)
        }
    }

    /**
     * Called when the library is updated. It sets the new data and updates the view.
     *
     * @param categories the categories of the library.
     * @param mangaMap a map containing the manga for each category.
     */
    fun onNextLibraryUpdate(categories: List<Category>, mangaMap: Map<Int, List<Manga>>) {
        // Check if library is empty and update information accordingly.
        (activity as MainActivity).updateEmptyView(mangaMap.isEmpty(),
                R.string.information_empty_library, R.drawable.ic_book_black_128dp)

        // Get the current active category.
        val activeCat = if (adapter.categories.isNotEmpty()) view_pager.currentItem else activeCategory

        // Set the categories
        adapter.categories = categories
        tabs.visibility = if (categories.size <= 1) View.GONE else View.VISIBLE

        // Restore active category.
        view_pager.setCurrentItem(activeCat, false)
        // Delay the scroll position to allow the view to be properly measured.
        view_pager.post { if (isAdded) tabs.setScrollPosition(view_pager.currentItem, 0f, true) }

        // Send the manga map to child fragments after the adapter is updated.
        presenter.libraryMangaSubject.call(LibraryMangaEvent(mangaMap))
    }

    /**
     * Creates the action mode if it's not created already.
     */
    fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = activity.startSupportActionMode(this)
        }
    }

    /**
     * Destroys the action mode.
     */
    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
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
        val count = presenter.selectedMangas.size
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = getString(R.string.label_selected, count)
            menu.findItem(R.id.action_edit_cover)?.isVisible = count == 1
        }
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_edit_cover -> {
                changeSelectedCover(presenter.selectedMangas)
                destroyActionModeIfNeeded()
            }
            R.id.action_move_to_category -> moveMangasToCategories(presenter.selectedMangas)
            R.id.action_delete -> showDeleteMangaDialog()
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        presenter.clearSelections()
        actionMode = null
    }

    /**
     * Changes the cover for the selected manga.
     *
     * @param mangas a list of selected manga.
     */
    private fun changeSelectedCover(mangas: List<Manga>) {
        if (mangas.size == 1) {
            selectedCoverManga = mangas[0]
            if (selectedCoverManga?.favorite ?: false) {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(Intent.createChooser(intent,
                        getString(R.string.file_select_cover)), REQUEST_IMAGE_OPEN)
            } else {
                context.toast(R.string.notification_first_add_to_library)
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE_OPEN) {
            selectedCoverManga?.let { manga ->

                try {
                    // Get the file's input stream from the incoming Intent
                    context.contentResolver.openInputStream(data.data).use {
                        // Update cover to selected file, show error if something went wrong
                        if (presenter.editCoverWithStream(it, manga)) {
                            // TODO refresh cover
                        } else {
                            context.toast(R.string.notification_manga_update_failed)
                        }
                    }
                } catch (error: IOException) {
                    context.toast(R.string.notification_manga_update_failed)
                    Timber.e(error)
                }
            }

        }
    }

    /**
     * Move the selected manga to a list of categories.
     *
     * @param mangas the manga list to move.
     */
    private fun moveMangasToCategories(mangas: List<Manga>) {
        // Hide the default category because it has a different behavior than the ones from db.
        val categories = presenter.categories.filter { it.id != 0 }

        // Get indexes of the common categories to preselect.
        val commonCategoriesIndexes = presenter.getCommonCategories(mangas)
                .map { categories.indexOf(it) }
                .toTypedArray()

        MaterialDialog.Builder(activity)
                .title(R.string.action_move_category)
                .items(categories.map { it.name })
                .itemsCallbackMultiChoice(commonCategoriesIndexes) { dialog, positions, text ->
                    val selectedCategories = positions.map { categories[it] }
                    presenter.moveMangasToCategories(selectedCategories, mangas)
                    destroyActionModeIfNeeded()
                    true
                }
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .show()
    }

    private fun showDeleteMangaDialog() {
        MaterialDialog.Builder(activity)
                .content(R.string.confirm_delete_manga)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive { dialog, action ->
                    presenter.removeMangaFromLibrary()
                    destroyActionModeIfNeeded()
                }
                .show()
    }

}
