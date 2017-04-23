package eu.kanade.tachiyomi.ui.catalogue

import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.f2prateek.rx.preferences.Preference
import com.jakewharton.rxbinding.widget.itemSelections
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.SecondaryDrawerController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.connectivityManager
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.snack
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_catalogue.view.*
import kotlinx.android.synthetic.main.toolbar.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Fragment that shows the manga from the catalogue.
 * Uses R.layout.fragment_catalogue.
 */
open class CatalogueController(bundle: Bundle? = null) :
        NucleusController<CataloguePresenter>(bundle),
        SecondaryDrawerController,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        FlexibleAdapter.EndlessScrollListener<ProgressItem> {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Spinner shown in the toolbar to change the selected source.
     */
    private var spinner: Spinner? = null

    /**
     * Adapter containing the list of manga from the catalogue.
     */
    private lateinit var adapter: FlexibleAdapter<IFlexible<*>>

    /**
     * Query of the search box.
     */
    private val query: String
        get() = presenter.query

    /**
     * Selected index of the spinner (selected source).
     */
    private var selectedIndex: Int = 0

    /**
     * Time in milliseconds to wait for input events in the search query before doing network calls.
     */
    private val SEARCH_TIMEOUT = 1000L

    /**
     * Subject to debounce the query.
     */
    private val queryDebouncerSubject = PublishSubject.create<String>()

    /**
     * Subscription of the number of manga per row.
     */
    private var numColumnsSubscription: Subscription? = null

    /**
     * Search item.
     */
    private var searchItem: MenuItem? = null

    /**
     * Property to get the toolbar from the containing activity.
     */
    private val toolbar: Toolbar
        get() = (activity as MainActivity).toolbar

    private var drawer: DrawerLayout? = null

    /**
     * Snackbar containing an error message when a request fails.
     */
    private var snack: Snackbar? = null

    /**
     * Navigation view containing filter items.
     */
    private var navView: CatalogueNavigationView? = null

    /**
     * Drawer listener to allow swipe only for closing the drawer.
     */
    private val drawerListener by lazy {
        object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                if (drawerView == navView) {
                    drawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navView)
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                if (drawerView == navView) {
                    drawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, navView)
                }
            }
        }
    }

    lateinit var recycler: RecyclerView

    private var progressItem: ProgressItem? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return ""
    }

    override fun createPresenter(): CataloguePresenter {
        return CataloguePresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.fragment_catalogue, container, false)
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        // Initialize adapter, scroll listener and recycler views
        adapter = FlexibleAdapter(null, this)
        setupRecycler(view)

        // Create toolbar spinner
        val themedContext = (activity as AppCompatActivity).supportActionBar?.themedContext ?: activity

        val spinnerAdapter = ArrayAdapter(themedContext,
                android.R.layout.simple_spinner_item, presenter.sources)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)

        val onItemSelected: (Int) -> Unit = { position ->
            val source = spinnerAdapter.getItem(position)
            if (!presenter.isValidSource(source)) {
                spinner?.setSelection(selectedIndex)
                activity?.toast(R.string.source_requires_login)
            } else if (source != presenter.source) {
                selectedIndex = position
                showProgressBar()
                adapter.clear()
                presenter.setActiveSource(source)
                navView?.setFilters(presenter.filterItems)
                activity?.invalidateOptionsMenu()
            }
        }

        selectedIndex = presenter.sources.indexOf(presenter.source)

        spinner = Spinner(themedContext).apply {
            adapter = spinnerAdapter
            setSelection(selectedIndex)
            itemSelections()
                    .skip(1)
                    .filter { it != AdapterView.INVALID_POSITION }
                    .subscribeUntilDestroy { onItemSelected(it) }
        }

        activity?.toolbar?.addView(spinner)

//        showProgressBar()
        view.progress?.visibility = ProgressBar.VISIBLE
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        val activity = activity!!
        with(view) {
            navView?.let {
                activity.drawer.removeDrawerListener(drawerListener)
                activity.drawer.removeView(it)
            }
            numColumnsSubscription?.unsubscribe()
            searchItem?.let {
                if (it.isActionViewExpanded) it.collapseActionView()
            }
            spinner?.let { toolbar.removeView(it) }
        }
        spinner = null
        navView = null
        searchItem = null
    }

    override fun createSecondaryDrawer(drawer: DrawerLayout): ViewGroup {
        // Inflate and prepare drawer
        val navView = drawer.inflate(R.layout.catalogue_drawer) as CatalogueNavigationView
        this.navView = navView
        this.drawer = drawer
        drawer.addDrawerListener(drawerListener)
        navView.setFilters(presenter.filterItems)

        navView.post {
            val activity = activity
            if (activity != null && !activity.drawer.isDrawerOpen(navView))
                activity.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navView)
        }

        navView.onSearchClicked = {
            val allDefault = presenter.sourceFilters == presenter.source.getFilterList()
            showProgressBar()
            adapter.clear()
            presenter.setSourceFilter(if (allDefault) FilterList() else presenter.sourceFilters)
        }

        navView.onResetClicked = {
            presenter.appliedFilters = FilterList()
            val newFilters = presenter.source.getFilterList()
            presenter.sourceFilters = newFilters
            navView.setFilters(presenter.filterItems)
        }
        return navView
    }

    override fun cleanupSecondaryDrawer(drawer: DrawerLayout) {
        drawer.removeDrawerListener(drawerListener)
        this.drawer = null
        navView = null
    }

    private fun setupRecycler(view: View) {
        numColumnsSubscription?.unsubscribe()

        var oldPosition = RecyclerView.NO_POSITION
        with(view) {
            val oldRecycler = catalogue_view?.getChildAt(1)
            if (oldRecycler is RecyclerView) {
                oldPosition = (oldRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                oldRecycler.adapter = null

                catalogue_view?.removeView(oldRecycler)
            }
        }

        recycler = if (presenter.isListMode) {
            RecyclerView(view.context).apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        } else {
            (view.catalogue_view.inflate(R.layout.recycler_autofit) as AutofitRecyclerView).apply {
                numColumnsSubscription = getColumnsPreferenceForCurrentOrientation().asObservable()
                        .doOnNext { spanCount = it }
                        .skip(1)
                        // Set again the adapter to recalculate the covers height
                        .subscribe { adapter = this@CatalogueController.adapter }

                (layoutManager as GridLayoutManager).spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter?.getItemViewType(position)) {
                            R.layout.item_catalogue_grid, null -> 1
                            else -> spanCount
                        }
                    }
                }
            }
        }
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter

        view.catalogue_view.addView(recycler, 1)

        if (oldPosition != RecyclerView.NO_POSITION) {
            recycler.layoutManager.scrollToPosition(oldPosition)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.catalogue_list, menu)

        // Initialize search menu
        searchItem = menu.findItem(R.id.action_search).apply {
            val searchView = actionView as SearchView

            if (!query.isBlank()) {
                expandActionView()
                searchView.setQuery(query, true)
                searchView.clearFocus()
            }
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    onSearchEvent(query, true)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    onSearchEvent(newText, false)
                    return true
                }
            })
        }

        // Setup filters button
        menu.findItem(R.id.action_set_filter).apply {
            icon.mutate()
            if (presenter.sourceFilters.isEmpty()) {
                isEnabled = false
                icon.alpha = 128
            } else {
                isEnabled = true
                icon.alpha = 255
            }
        }

        // Show next display mode
        menu.findItem(R.id.action_display_mode).apply {
            val icon = if (presenter.isListMode)
                R.drawable.ic_view_module_white_24dp
            else
                R.drawable.ic_view_list_white_24dp
            setIcon(icon)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_display_mode -> swapDisplayMode()
            R.id.action_set_filter -> navView?.let { activity?.drawer?.openDrawer(Gravity.END) }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        queryDebouncerSubject.debounce(SEARCH_TIMEOUT, MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeUntilDetach { searchWithQuery(it) }
    }

    /**
     * Called when the input text changes or is submitted.
     *
     * @param query the new query.
     * @param now whether to send the network call now or debounce it by [SEARCH_TIMEOUT].
     */
    private fun onSearchEvent(query: String, now: Boolean) {
        if (now) {
            searchWithQuery(query)
        } else {
            queryDebouncerSubject.onNext(query)
        }
    }

    /**
     * Restarts the request with a new query.
     *
     * @param newQuery the new query.
     */
    private fun searchWithQuery(newQuery: String) {
        // If text didn't change, do nothing
        if (query == newQuery)
            return

        showProgressBar()
        adapter.clear()

        presenter.restartPager(newQuery)
    }

    /**
     * Called from the presenter when the network request is received.
     *
     * @param page the current page.
     * @param mangas the list of manga of the page.
     */
    fun onAddPage(page: Int, mangas: List<CatalogueItem>) {
        hideProgressBar()
        if (page == 1) {
            adapter.clear()
            resetProgressItem()
        }
        adapter.onLoadMoreComplete(mangas)
    }

    /**
     * Called from the presenter when the network request fails.
     *
     * @param error the error received.
     */
    fun onAddPageError(error: Throwable) {
        adapter.onLoadMoreComplete(null)
        hideProgressBar()

        val message = if (error is NoResultsException) "No results found" else (error.message ?: "")

        snack?.dismiss()
        snack = view?.catalogue_view?.snack(message, Snackbar.LENGTH_INDEFINITE) {
            setAction(R.string.action_retry) {
                // If not the first page, show bottom progress bar.
                if (adapter.mainItemCount > 0) {
                    val item = progressItem ?: return@setAction
                    adapter.addScrollableFooterWithDelay(item, 0, true)
                } else {
                    showProgressBar()
                }
                presenter.requestNext()
            }
        }
    }

    /**
     * Sets a new progress item and reenables the scroll listener.
     */
    private fun resetProgressItem() {
        progressItem = ProgressItem()
        adapter.endlessTargetCount = 0
        adapter.setEndlessScrollListener(this, progressItem!!)
    }

    /**
     * Called by the adapter when scrolled near the bottom.
     */
    override fun onLoadMore(lastPosition: Int, currentPage: Int) {
        if (presenter.hasNextPage()) {
            presenter.requestNext()
        } else {
            adapter.onLoadMoreComplete(null)
            adapter.endlessTargetCount = 1
        }
    }

    override fun noMoreLoad(newItemsSize: Int) {
    }

    /**
     * Called from the presenter when a manga is initialized.
     *
     * @param manga the manga initialized
     */
    fun onMangaInitialized(manga: Manga) {
        getHolder(manga)?.setImage(manga)
    }

    /**
     * Swaps the current display mode.
     */
    fun swapDisplayMode() {
        val view = view ?: return

        presenter.swapDisplayMode()
        val isListMode = presenter.isListMode
        activity?.invalidateOptionsMenu()
        setupRecycler(view)
        if (!isListMode || !view.context.connectivityManager.isActiveNetworkMetered) {
            // Initialize mangas if going to grid view or if over wifi when going to list view
            val mangas = (0..adapter.itemCount-1).mapNotNull {
                (adapter.getItem(it) as? CatalogueItem)?.manga
            }
            presenter.initializeMangas(mangas)
        }
    }

    /**
     * Returns a preference for the number of manga per row based on the current orientation.
     *
     * @return the preference.
     */
    fun getColumnsPreferenceForCurrentOrientation(): Preference<Int> {
        return if (resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT)
            presenter.prefs.portraitColumns()
        else
            presenter.prefs.landscapeColumns()
    }

    /**
     * Returns the view holder for the given manga.
     *
     * @param manga the manga to find.
     * @return the holder of the manga or null if it's not bound.
     */
    private fun getHolder(manga: Manga): CatalogueHolder? {
        adapter.allBoundViewHolders.forEach { holder ->
            val item = adapter.getItem(holder.adapterPosition) as? CatalogueItem
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as CatalogueHolder
            }
        }

        return null
    }

    /**
     * Shows the progress bar.
     */
    private fun showProgressBar() {
        view?.progress?.visibility = ProgressBar.VISIBLE
        snack?.dismiss()
        snack = null
    }

    /**
     * Hides active progress bars.
     */
    private fun hideProgressBar() {
        view?.progress?.visibility = ProgressBar.GONE
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onItemClick(position: Int): Boolean {
        val item = adapter.getItem(position) as? CatalogueItem ?: return false
        router.pushController(RouterTransaction.with(MangaController(item.manga))
                .pushChangeHandler(FadeChangeHandler())
                .popChangeHandler(FadeChangeHandler()))

        return false
    }

    /**
     * Called when a manga is long clicked.
     *
     * Adds the manga to the default category if none is set it shows a list of categories for the user to put the manga
     * in, the list consists of the default category plus the user's categories. The default category is preselected on
     * new manga, and on already favorited manga the manga's categories are preselected.
     *
     * @param position the position of the element clicked.
     */
    override fun onItemLongClick(position: Int) {
//        // Get manga
//        val manga = (adapter.getItem(position) as? CatalogueItem?)?.manga ?: return
//        // Fetch categories
//        val categories = presenter.getCategories()
//
//        if (manga.favorite){
//            MaterialDialog.Builder(activity)
//                    .items(getString(R.string.remove_from_library ))
//                    .itemsCallback { _, _, which, _ ->
//                        when (which) {
//                            0 -> {
//                                presenter.changeMangaFavorite(manga)
//                                adapter.notifyItemChanged(position)
//                            }
//                        }
//                    }.show()
//        }else{
//            val defaultCategory = categories.find { it.id == preferences.defaultCategory()}
//            if(defaultCategory != null) {
//                presenter.changeMangaFavorite(manga)
//                presenter.moveMangaToCategory(defaultCategory, manga)
//                // Show manga has been added
//                context.toast(R.string.added_to_library)
//                adapter.notifyItemChanged(position)
//            } else {
//                MaterialDialog.Builder(activity)
//                        .title(R.string.action_move_category)
//                        .items(categories.map { it.name })
//                        .itemsCallbackMultiChoice(presenter.getMangaCategoryIds(manga)) { dialog, position, _ ->
//                            if (position.contains(0) && position.count() > 1) {
//                                // Deselect default category
//                                dialog.setSelectedIndices(position.filter {it > 0}.toTypedArray())
//                                dialog.context.toast(R.string.invalid_combination)
//                            }
//                            true
//                        }
//                        .alwaysCallMultiChoiceCallback()
//                        .positiveText(android.R.string.ok)
//                        .negativeText(android.R.string.cancel)
//                        .onPositive { dialog, _ ->
//                            val selectedCategories = dialog.selectedIndices?.map { categories[it] } ?: emptyList()
//                            updateMangaCategories(manga, selectedCategories, position)
//                        }
//                        .build()
//                        .show()
//            }
//        }
    }

    /**
     * Update manga to use selected categories.
     *
     * @param manga needed to change
     * @param selectedCategories selected categories
     * @param position position of adapter
     */
    private fun updateMangaCategories(manga: Manga, selectedCategories: List<Category>, position: Int) {
        presenter.updateMangaCategories(manga,selectedCategories)
        adapter.notifyItemChanged(position)
    }

}
