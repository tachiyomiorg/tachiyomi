package eu.kanade.tachiyomi.ui.library

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.view.ActionMode
import android.support.v7.widget.SearchView
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.category.CategoryActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_library.*
import nucleus.factory.RequiresPresenter
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
     * Display mode of the library (list or grid mode).
     */
    private var displayMode: MenuItem? = null

    /**
     * Action mode for manga selection.
     */
    var actionMode: ActionMode? = null
        private set

    /**
     * Selected manga for editing its cover.
     */
    private var selectedCoverManga: Manga? = null

    /**
     * Status of isFilterDownloaded
     */
    var isFilterDownloaded = false

    /**
     * Status of isFilterUnread
     */
    var isFilterUnread = false

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
        isFilterDownloaded = presenter.preferences.filterDownloaded().get() as Boolean
        isFilterUnread = presenter.preferences.filterUnread().get() as Boolean
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        setToolbarTitle(getString(R.string.label_library))

        adapter = LibraryAdapter(childFragmentManager)
        view_pager.adapter = adapter
        view_pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                presenter.preferences.lastUsedCategory().set(position)
            }
        })
        tabs.setupWithViewPager(view_pager)

        if (savedState != null) {
            activeCategory = savedState.getInt(CATEGORY_KEY)
            query = savedState.getString(QUERY_KEY)
            presenter.searchSubject.onNext(query)
        } else {
            activeCategory = presenter.preferences.lastUsedCategory().getOrDefault()
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.subscribeLibrary()
    }

    override fun onDestroyView() {
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

        // Initialize search menu
        val filterDownloadedItem = menu.findItem(R.id.action_filter_downloaded)
        val filterUnreadItem = menu.findItem(R.id.action_filter_unread)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        if (!query.isNullOrEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }

        filterDownloadedItem.isChecked = isFilterDownloaded
        filterUnreadItem.isChecked = isFilterUnread

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

        //set the icon for the display mode button
        displayMode = menu.findItem(R.id.action_library_display_mode).apply {
            val icon = if (preferences.libraryAsList().getOrDefault())
                R.drawable.ic_view_module_white_24dp
            else
                R.drawable.ic_view_list_white_24dp

            setIcon(icon)
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter_unread -> {
                // Change unread filter status.
                isFilterUnread = !isFilterUnread
                // Update settings.
                presenter.preferences.filterUnread().set(isFilterUnread)
                // Apply filter.
                onFilterCheckboxChanged()
            }
            R.id.action_filter_downloaded -> {
                // Change downloaded filter status.
                isFilterDownloaded = !isFilterDownloaded
                // Update settings.
                presenter.preferences.filterDownloaded().set(isFilterDownloaded)
                // Apply filter.
                onFilterCheckboxChanged()
            }
            R.id.action_filter_empty -> {
                // Remove filter status.
                isFilterUnread = false
                isFilterDownloaded = false
                // Update settings.
                presenter.preferences.filterUnread().set(isFilterUnread)
                presenter.preferences.filterDownloaded().set(isFilterDownloaded)
                // Apply filter
                onFilterCheckboxChanged()
            }
            R.id.action_library_display_mode -> swapDisplayMode()
            R.id.action_update_library -> {
                LibraryUpdateService.start(activity, true)
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
     * Applies filter change
     */
    private fun onFilterCheckboxChanged() {
        presenter.updateLibrary()
        adapter.notifyDataSetChanged()
        adapter.refreshRegisteredAdapters()
        activity.supportInvalidateOptionsMenu()
    }

    /**
     * swap display mode
     */
    private fun swapDisplayMode() {

        presenter.swapDisplayMode()
        val isListMode = presenter.displayAsList
        val icon = if (isListMode)
            R.drawable.ic_view_module_white_24dp
        else
            R.drawable.ic_view_list_white_24dp

        displayMode?.setIcon(icon)

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
            presenter.searchSubject.onNext(query)
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
        val activeCat = if (adapter.categories != null) view_pager.currentItem else activeCategory

        // Set the categories
        adapter.categories = categories
        tabs.visibility = if (categories.size <= 1) View.GONE else View.VISIBLE

        // Restore active category.
        view_pager.setCurrentItem(activeCat, false)
        // Delay the scroll position to allow the view to be properly measured.
        view_pager.post { if (isAdded) tabs.setScrollPosition(view_pager.currentItem, 0f, true) }

        // Send the manga map to child fragments after the adapter is updated.
        presenter.libraryMangaSubject.onNext(LibraryMangaEvent(mangaMap))
    }

    /**
     * Sets the title of the action mode.
     *
     * @param count the number of items selected.
     */
    fun setContextTitle(count: Int) {
        actionMode?.title = getString(R.string.label_selected, count)
    }

    /**
     * Sets the visibility of the edit cover item.
     *
     * @param count the number of items selected.
     */
    fun setVisibilityOfCoverEdit(count: Int) {
        // If count = 1 display edit button
        actionMode?.menu?.findItem(R.id.action_edit_cover)?.isVisible = count == 1
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.library_selection, menu)
        adapter.setSelectionMode(FlexibleAdapter.MODE_MULTI)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
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
        adapter.setSelectionMode(FlexibleAdapter.MODE_SINGLE)
        presenter.selectedMangas.clear()
        actionMode = null
    }

    /**
     * Destroys the action mode.
     */
    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
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
                            adapter.refreshRegisteredAdapters()
                        } else {
                            context.toast(R.string.notification_manga_update_failed)
                        }
                    }
                } catch (e: IOException) {
                    context.toast(R.string.notification_manga_update_failed)
                    e.printStackTrace()
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

    /**
     * Creates the action mode if it's not created already.
     */
    fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = activity.startSupportActionMode(this)
        }
    }

    /**
     * Invalidates the action mode, forcing it to refresh its content.
     */
    fun invalidateActionMode() {
        actionMode?.invalidate()
    }

}
