package eu.kanade.tachiyomi.ui.library

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.TabLayout
import android.support.v7.view.ActionMode
import android.support.v7.widget.SearchView
import android.view.*
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.io.downloadMediaAndReturnPath
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.event.LibraryMangasEvent
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.category.CategoryActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.ToastUtil
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.setInformationDrawable
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_library.*
import nucleus.factory.RequiresPresenter
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileInputStream
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
     * TabLayout of the categories.
     */
    private lateinit var tabs: TabLayout

    /**
     * AppBarLayout from [MainActivity].
     */
    private lateinit var appBar: AppBarLayout

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
    var actionMode: ActionMode? = null
        private set

    /**
     * Selected manga for editing its cover.
     */
    private var selectedCoverManga: Manga? = null


    /**
     * TODO
     */
    var isFilterDownloaded = false

    /**
     * TODO
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
        @JvmStatic
        fun newInstance(): LibraryFragment {
            return LibraryFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        isFilterDownloaded = presenter.preferences.filterDownloaded().get() as Boolean
        isFilterUnread = presenter.preferences.filterUnread().get() as Boolean
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        setToolbarTitle(getString(R.string.label_library))
        ButterKnife.bind(this, view)

        appBar = (activity as MainActivity).appbar
        tabs = appBar.inflate(R.layout.library_tab_layout) as TabLayout

        // Workaround to prevent: Tab belongs to a different TabLayout.
        // Internal bug in Support library v23.2.0.
        // See https://code.google.com/p/android/issues/detail?id=201827
        for (j in 0..16) {
            tabs.newTab()
        }

        appBar.addView(tabs)

        adapter = LibraryAdapter(childFragmentManager)
        view_pager.adapter = adapter
        tabs.setupWithViewPager(view_pager)

        if (savedState != null) {
            activeCategory = savedState.getInt(CATEGORY_KEY)
            query = savedState.getString(QUERY_KEY)
            presenter.searchSubject.onNext(query)
        }
    }

    override fun onDestroyView() {
        appBar.removeView(tabs)
        super.onDestroyView()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        bundle.putInt(CATEGORY_KEY, view_pager.currentItem)
        bundle.putString(QUERY_KEY, query)
        super.onSaveInstanceState(bundle)
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

        filterDownloadedItem.isChecked = isFilterDownloaded;
        filterUnreadItem.isChecked = isFilterUnread;

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
            R.id.action_refresh -> LibraryUpdateService.start(activity)
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
        activity.supportInvalidateOptionsMenu();
    }

    /**
     * Updates the query.
     *
     * @param query the new value of the query.
     */
    private fun onSearchTextChange(query: String?) {
        this.query = query

        // Notify the subject the query has changed.
        presenter.searchSubject.onNext(query)
    }

    /**
     * Called when the library is updated. It sets the new data and updates the view.
     *
     * @param categories the categories of the library.
     * @param mangaMap a map containing the manga for each category.
     */
    fun onNextLibraryUpdate(categories: List<Category>, mangaMap: Map<Int, List<Manga>>) {
        // Check if library is empty and update information accordingly.
        if (mangaMap.isEmpty()) {
            (activity as MainActivity).image_view.setInformationDrawable(R.drawable.ic_book_grey_128dp)
            (activity as MainActivity).text_label.text = getString(R.string.information_empty_library)
        } else {
            ( activity as MainActivity).image_view.setInformationDrawable(null)
            ( activity as MainActivity).text_label.text = ""
        }

        // Get the current active category.
        val activeCat = if (adapter.categories != null) view_pager.currentItem else activeCategory

        // Add the default category if it contains manga.
        if (mangaMap[0] != null) {
            setCategories(arrayListOf(Category.createDefault()) + categories)
        } else {
            setCategories(categories)
        }

        // Restore active category.
        view_pager.setCurrentItem(activeCat, false)
        if (tabs.tabCount > 0) {
            // Prevent IndexOutOfBoundsException
            if (tabs.tabCount <= view_pager.currentItem) {
                view_pager.currentItem = (tabs.tabCount - 1)
            }
            tabs.getTabAt(view_pager.currentItem)?.select()
        }

        // Send the manga map to child fragments after the adapter is updated.
        EventBus.getDefault().postSticky(LibraryMangasEvent(mangaMap))
    }

    /**
     * Sets the categories in the adapter and the tab layout.
     *
     * @param categories the categories to set.
     */
    private fun setCategories(categories: List<Category>) {
        adapter.categories = categories
        tabs.setupWithViewPager(view_pager)
        tabs.visibility = if (categories.size <= 1) View.GONE else View.VISIBLE
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
            R.id.action_move_to_category -> {
                moveMangasToCategories(presenter.selectedMangas)
            }
            R.id.action_delete -> {
                presenter.deleteMangas()
                destroyActionModeIfNeeded()
            }
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
                ToastUtil.showShort(context, R.string.notification_first_add_to_library)
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE_OPEN) {
            selectedCoverManga?.let { manga ->

                try {
                    // Get the file's input stream from the incoming Intent
                    val inputStream = context.contentResolver.openInputStream(data.data)

                    // Convert to absolute path to prevent FileNotFoundException
                    val result = downloadMediaAndReturnPath(inputStream as FileInputStream, context)

                    // Get file from filepath
                    val picture = File(result)

                    // Update cover to selected file, show error if something went wrong
                    if (presenter.editCoverWithLocalFile(picture, manga)) {
                        adapter.refreshRegisteredAdapters()
                    } else {
                        context.toast(R.string.notification_manga_update_failed)
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
        MaterialDialog.Builder(activity)
                .title(R.string.action_move_category)
                .items(presenter.getCategoryNames())
                .itemsCallbackMultiChoice(null) { dialog, positions, text ->
                    presenter.moveMangasToCategories(positions, mangas)
                    destroyActionModeIfNeeded()
                    true
                }
                .positiveText(R.string.button_ok)
                .negativeText(R.string.button_cancel)
                .show()
    }

    /**
     * Creates the action mode if it's not created already.
     */
    fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = baseActivity.startSupportActionMode(this)
        }
    }

    /**
     * Invalidates the action mode, forcing it to refresh its content.
     */
    fun invalidateActionMode() {
        actionMode?.invalidate()
    }

}
