package eu.kanade.tachiyomi.ui.library2

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.*
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.library.LibraryMangaEvent
import eu.kanade.tachiyomi.ui.library.LibraryNavigationView
import eu.kanade.tachiyomi.ui.main.MainActivity2
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.fragment_library.view.*
import uy.kohesive.injekt.injectLazy


class LibraryController(bundle: Bundle? = null) : NucleusController<LibraryPresenter>(bundle),
        ActionMode.Callback {

    private val preferences: PreferencesHelper by injectLazy()

    private var adapter: LibraryAdapter? = null

    /**
     * Position of the active category.
     */
    private var activeCategory = preferences.lastUsedCategory().getOrDefault()

    private var actionMode: ActionMode? = null

    /**
     * Number of manga per row in grid mode.
     */
    var mangaPerRow = 3
        private set

    /**
     * TabLayout of the categories.
     */
    private val tabs: TabLayout
        get() = (activity as MainActivity2).tabs

    private val drawer: DrawerLayout?
        get() = (activity as? MainActivity2)?.drawer

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

    override fun createPresenter(): LibraryPresenter {
        return LibraryPresenter()
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_library)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        adapter = LibraryAdapter(this)
        with(view) {
            view_pager.adapter = adapter
            view_pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    preferences.lastUsedCategory().set(position)
                }
            })
            tabs.setupWithViewPager(view_pager)

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
        adapter = null
        drawer?.removeDrawerListener(drawerListener)
        drawer?.removeView(navView)
        with(view) {
            tabs.setupWithViewPager(null)
            tabs.visibility = View.GONE
        }
    }

    fun onNextLibraryUpdate(categories: List<Category>, mangaMap: Map<Int, List<Manga>>) {
        val adapter = adapter ?: return
        withView {
            // Get the current active category.
            val activeCat = if (adapter.categories.isNotEmpty()) view_pager.currentItem else activeCategory

            // Set the categories
            adapter.categories = categories
            tabs.visibility = if (categories.size <= 1) View.GONE else View.VISIBLE

            // Restore active category.
            view_pager.setCurrentItem(activeCat, false)
            // Delay the scroll position to allow the view to be properly measured.
            view_pager.post { if (isAttached) tabs.setScrollPosition(view_pager.currentItem, 0f, true) }

            // Send the manga map to child fragments after the adapter is updated.
            presenter.libraryMangaSubject.call(LibraryMangaEvent(mangaMap))
        }
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
        val adapter = adapter ?: return
        with(view!!) {
            val position = view_pager.currentItem
            adapter.recycle = false
            view_pager.adapter = adapter
            view_pager.currentItem = position
            adapter.recycle = true
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