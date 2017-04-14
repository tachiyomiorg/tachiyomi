package eu.kanade.tachiyomi.ui.library2

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.SearchView
import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.jakewharton.rxbinding.support.v7.widget.queryTextChanges
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.library.LibraryMangaEvent
import eu.kanade.tachiyomi.ui.library.LibraryNavigationView
import eu.kanade.tachiyomi.ui.main.MainActivity2
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.activity_main2.*
import uy.kohesive.injekt.injectLazy


class LibraryController(bundle: Bundle? = null) : NucleusController<LibraryPresenter>(bundle),
        ActionMode.Callback {

    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Position of the active category.
     */
    var activeCategory = preferences.lastUsedCategory().getOrDefault()
        private set

    private var actionMode: ActionMode? = null

    /**
     * Number of manga per row in grid mode.
     */
    var mangaPerRow = 3
        private set

    private val ui
        get() = view as? LibraryView

    /**
     * TabLayout of the categories.
     */
    private val tabs: TabLayout?
        get() = (activity as? MainActivity2)?.tabs

    private val drawer: DrawerLayout?
        get() = (activity as? MainActivity2)?.drawer

    private var query = ""

    /**
     * Navigation view containing filter/sort/display items.
     */
    private var navView: LibraryNavigationView? = null

    /**
     * Drawer listener to allow swipe only for closing the drawer.
     */
    private val drawerListener by lazy {
        object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                if (drawerView == navView) {
                    activity?.drawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navView)
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                if (drawerView == navView) {
                    activity?.drawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, navView)
                }
            }
        }
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun createPresenter(): LibraryPresenter {
        return LibraryPresenter()
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_library)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return LibraryView(this)
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)
        with(view as ViewPager) {
            addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    preferences.lastUsedCategory().set(position)
                    activeCategory = position
                }
            })
            tabs?.setupWithViewPager(this)

            // Inflate and prepare drawer
            navView = drawer?.inflate(R.layout.library_drawer) as LibraryNavigationView
            drawer?.addView(navView)
            drawer?.addDrawerListener(drawerListener)

            navView?.post {
                val drawer = drawer ?: return@post
                if (drawer.isDrawerOpen(navView))
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navView)
            }

            navView?.onGroupClicked = { group ->
                when (group) {
                    is LibraryNavigationView.FilterGroup -> onFilterChanged()
                    is LibraryNavigationView.SortGroup -> onSortChanged()
                    is LibraryNavigationView.DisplayGroup -> reattachAdapter()
                }
            }
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.subscribeLibrary()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        drawer?.removeDrawerListener(drawerListener)
        drawer?.removeView(navView)
        tabs?.setupWithViewPager(null)
        tabs?.visibility = View.GONE
    }

    fun onNextLibraryUpdate(categories: List<Category>, mangaMap: Map<Int, List<Manga>>) {
        ui?.setCategories(categories)

        tabs?.visibility = if (categories.size <= 1) View.GONE else View.VISIBLE
        // Delay the scroll position to allow the view to be properly measured.
        ui?.post { tabs?.setScrollPosition(ui!!.currentItem, 0f, true) }

        // Send the manga map to child fragments after the adapter is updated.
        presenter.libraryMangaSubject.call(LibraryMangaEvent(mangaMap))
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
        ui?.reattachAdapter()
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
            presenter.searchSubject.call(query)
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
                drawer?.openDrawer(Gravity.END)
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
        val count = presenter.selectedMangas.size
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
//            R.id.action_edit_cover -> {
//                changeSelectedCover(presenter.selectedMangas)
//                destroyActionModeIfNeeded()
//            }
//            R.id.action_move_to_category -> moveMangasToCategories(presenter.selectedMangas)
//            R.id.action_delete -> showDeleteMangaDialog()
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        presenter.clearSelections()
        actionMode = null
    }

}