package eu.kanade.tachiyomi.ui.catalogue

import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.*
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import com.afollestad.materialdialogs.MaterialDialog
import com.f2prateek.rx.preferences.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.util.connectivityManager
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.snack
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_catalogue.*
import kotlinx.android.synthetic.main.toolbar.*
import nucleus.factory.RequiresPresenter
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Fragment that shows the manga from the catalogue.
 * Uses R.layout.fragment_catalogue.
 */
@RequiresPresenter(CataloguePresenter::class)
open class CatalogueFragment : BaseRxFragment<CataloguePresenter>(),
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        FlexibleAdapter.EndlessScrollListener<ProgressItem> {

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
     * Subscription of the debouncer subject.
     */
    private var queryDebouncerSubscription: Subscription? = null

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

    lateinit var recycler: RecyclerView

    private var progressItem: ProgressItem? = null

    companion object {
        /**
         * Creates a new instance of this fragment.
         *
         * @return a new instance of [CatalogueFragment].
         */
        fun newInstance(): CatalogueFragment {
            return CatalogueFragment()
        }
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_catalogue, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        // If the source list is empty or it only has unlogged sources, return to main screen.
        val sources = presenter.sources
        if (sources.isEmpty() || sources.all { it is LoginSource && !it.isLogged() }) {
            context.toast(R.string.no_valid_sources)
            activity.onBackPressed()
            return
        }

        // Initialize adapter, scroll listener and recycler views
        adapter = FlexibleAdapter(null, this)
        setupRecycler()

        // Create toolbar spinner
        val themedContext = activity.supportActionBar?.themedContext ?: activity

        val spinnerAdapter = ArrayAdapter(themedContext,
                android.R.layout.simple_spinner_item, presenter.sources)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)

        val onItemSelected = IgnoreFirstSpinnerListener { position ->
            val source = spinnerAdapter.getItem(position)
            if (!presenter.isValidSource(source)) {
                spinner?.setSelection(selectedIndex)
                context.toast(R.string.source_requires_login)
            } else if (source != presenter.source) {
                selectedIndex = position
                showProgressBar()
                adapter.clear()
                presenter.setActiveSource(source)
                navView?.setFilters(presenter.filterItems)
                activity.invalidateOptionsMenu()
            }
        }

        selectedIndex = presenter.sources.indexOf(presenter.source)

        spinner = Spinner(themedContext).apply {
            adapter = spinnerAdapter
            setSelection(selectedIndex)
            onItemSelectedListener = onItemSelected
        }

        setToolbarTitle("")
        toolbar.addView(spinner)

        // Inflate and prepare drawer
        val navView = activity.drawer.inflate(R.layout.catalogue_drawer) as CatalogueNavigationView
        this.navView = navView
        activity.drawer.addView(navView)
        activity.drawer.addDrawerListener(drawerListener)
        navView.setFilters(presenter.filterItems)

        navView.post {
            if (isAdded && !activity.drawer.isDrawerOpen(navView))
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

        showProgressBar()
    }

    private fun setupRecycler() {
        if (!isAdded) return

        numColumnsSubscription?.unsubscribe()

        val oldRecycler = catalogue_view.getChildAt(1)
        var oldPosition = RecyclerView.NO_POSITION
        if (oldRecycler is RecyclerView) {
            oldPosition = (oldRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            oldRecycler.adapter = null

            catalogue_view.removeView(oldRecycler)
        }

        recycler = if (presenter.isListMode) {
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        } else {
            (catalogue_view.inflate(R.layout.recycler_autofit) as AutofitRecyclerView).apply {
                numColumnsSubscription = getColumnsPreferenceForCurrentOrientation().asObservable()
                        .doOnNext { spanCount = it }
                        .skip(1)
                        // Set again the adapter to recalculate the covers height
                        .subscribe { adapter = this@CatalogueFragment.adapter }

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

        catalogue_view.addView(recycler, 1)

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
            R.id.action_set_filter -> navView?.let { activity.drawer.openDrawer(Gravity.END) }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        queryDebouncerSubscription = queryDebouncerSubject.debounce(SEARCH_TIMEOUT, MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { searchWithQuery(it) }
    }

    override fun onPause() {
        queryDebouncerSubscription?.unsubscribe()
        super.onPause()
    }

    override fun onDestroyView() {
        navView?.let {
            activity.drawer.removeDrawerListener(drawerListener)
            activity.drawer.removeView(it)
        }
        numColumnsSubscription?.unsubscribe()
        searchItem?.let {
            if (it.isActionViewExpanded) it.collapseActionView()
        }
        spinner?.let { toolbar.removeView(it) }
        super.onDestroyView()
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
        snack = catalogue_view.snack(message, Snackbar.LENGTH_INDEFINITE) {
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
        if (!isAdded) return

        presenter.swapDisplayMode()
        val isListMode = presenter.isListMode
        activity.invalidateOptionsMenu()
        setupRecycler()
        if (!isListMode || !context.connectivityManager.isActiveNetworkMetered) {
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
        return if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
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
        val layoutManager = recycler.layoutManager as LinearLayoutManager
        val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePos = layoutManager.findLastVisibleItemPosition()

        (firstVisiblePos..lastVisiblePos-1).forEach { i ->
            val item = adapter.getItem(i) as? CatalogueItem
            if (item != null && item.manga.id!! == manga.id!!) {
                return recycler.findViewHolderForLayoutPosition(i) as? CatalogueHolder
            }
        }

        return null
    }

    /**
     * Shows the progress bar.
     */
    private fun showProgressBar() {
        progress.visibility = ProgressBar.VISIBLE
        snack?.dismiss()
        snack = null
    }

    /**
     * Hides active progress bars.
     */
    private fun hideProgressBar() {
        progress.visibility = ProgressBar.GONE
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onItemClick(position: Int): Boolean {
        val item = adapter.getItem(position) as? CatalogueItem ?: return false

        val intent = MangaActivity.newIntent(activity, item.manga, true)
        startActivity(intent)
        return false
    }

    /**
     * Called when a manga is long clicked.
     *
     * @param position the position of the element clicked.
     */
    override fun onItemLongClick(position: Int) {
        val manga = (adapter.getItem(position) as? CatalogueItem?)?.manga ?: return

        val textRes = if (manga.favorite) R.string.remove_from_library else R.string.add_to_library

        MaterialDialog.Builder(activity)
                .items(getString(textRes))
                .itemsCallback { dialog, itemView, which, text ->
                    when (which) {
                        0 -> {
                            presenter.changeMangaFavorite(manga)
                            adapter.notifyItemChanged(position)
                        }
                    }
                }.show()
    }

}
