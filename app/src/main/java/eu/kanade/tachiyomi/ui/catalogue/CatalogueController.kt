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
import android.widget.Spinner
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.f2prateek.rx.preferences.Preference
import com.jakewharton.rxbinding.support.v7.widget.queryTextChangeEvents
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
import eu.kanade.tachiyomi.ui.library.ChangeMangaCategoriesDialog
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.*
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import eu.kanade.tachiyomi.widget.DrawerSwipeCloseListener
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.catalogue_controller.view.*
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.Subscriptions
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit

/**
 * Controller to manage the catalogues available in the app.
 */
open class CatalogueController(bundle: Bundle? = null) :
        NucleusController<CataloguePresenter>(bundle),
        SecondaryDrawerController,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        FlexibleAdapter.EndlessScrollListener<ProgressItem>,
        ChangeMangaCategoriesDialog.Listener {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Adapter containing the list of manga from the catalogue.
     */
    private var adapter: FlexibleAdapter<IFlexible<*>>? = null

    /**
     * Spinner shown in the toolbar to change the selected source.
     */
    private var spinner: Spinner? = null

    /**
     * Snackbar containing an error message when a request fails.
     */
    private var snack: Snackbar? = null

    /**
     * Navigation view containing filter items.
     */
    private var navView: CatalogueNavigationView? = null

    /**
     * Recycler view with the list of results.
     */
    private var recycler: RecyclerView? = null

    private var drawerListener: DrawerLayout.DrawerListener? = null

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
     * Subscription for the search view.
     */
    private var searchViewSubscription: Subscription? = null

    private var numColumnsSubscription: Subscription? = null

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
        return inflater.inflate(R.layout.catalogue_controller, container, false)
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        // Initialize adapter, scroll listener and recycler views
        adapter = FlexibleAdapter(null, this)
        setupRecycler(view)

        // Create toolbar spinner
        val themedContext = (activity as AppCompatActivity).supportActionBar?.themedContext
                ?: activity

        val spinnerAdapter = ArrayAdapter(themedContext,
                android.R.layout.simple_spinner_item, presenter.sources)
        spinnerAdapter.setDropDownViewResource(R.layout.common_spinner_item)

        val onItemSelected: (Int) -> Unit = { position ->
            val source = spinnerAdapter.getItem(position)
            if (!presenter.isValidSource(source)) {
                spinner?.setSelection(selectedIndex)
                activity?.toast(R.string.source_requires_login)
            } else if (source != presenter.source) {
                selectedIndex = position
                showProgressBar()
                adapter?.clear()
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

        view.progress?.visible()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        activity?.toolbar?.removeView(spinner)
        numColumnsSubscription?.unsubscribe()
        numColumnsSubscription = null
        searchViewSubscription = null
        adapter = null
        spinner = null
        snack = null
        recycler = null
    }

    override fun createSecondaryDrawer(drawer: DrawerLayout): ViewGroup? {
        // Inflate and prepare drawer
        val navView = drawer.inflate(R.layout.catalogue_drawer) as CatalogueNavigationView
        this.navView = navView
        drawerListener = DrawerSwipeCloseListener(drawer, navView).also {
            drawer.addDrawerListener(it)
        }
        navView.setFilters(presenter.filterItems)

        navView.post {
            if (isAttached && !drawer.isDrawerOpen(navView))
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navView)
        }

        navView.onSearchClicked = {
            val allDefault = presenter.sourceFilters == presenter.source.getFilterList()
            showProgressBar()
            adapter?.clear()
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
        drawerListener?.let { drawer.removeDrawerListener(it) }
        drawerListener = null
        navView = null
    }

    private fun setupRecycler(view: View) {
        numColumnsSubscription?.unsubscribe()

        var oldPosition = RecyclerView.NO_POSITION
            val oldRecycler = view.catalogue_view?.getChildAt(1)
            if (oldRecycler is RecyclerView) {
                oldPosition = (oldRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                oldRecycler.adapter = null

                view.catalogue_view?.removeView(oldRecycler)
            }

        val recycler = if (presenter.isListMode) {
            RecyclerView(view.context).apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        } else {
            (view.catalogue_view.inflate(R.layout.catalogue_recycler_autofit) as AutofitRecyclerView).apply {
                numColumnsSubscription = getColumnsPreferenceForCurrentOrientation().asObservable()
                        .doOnNext { spanCount = it }
                        .skip(1)
                        // Set again the adapter to recalculate the covers height
                        .subscribe { adapter = this@CatalogueController.adapter }

                (layoutManager as GridLayoutManager).spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter?.getItemViewType(position)) {
                            R.layout.catalogue_grid_item, null -> 1
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
        this.recycler = recycler
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.catalogue_list, menu)

        // Initialize search menu
        menu.findItem(R.id.action_search).apply {
            val searchView = actionView as SearchView

            if (!query.isBlank()) {
                expandActionView()
                searchView.setQuery(query, true)
                searchView.clearFocus()
            }

            val searchEventsObservable = searchView.queryTextChangeEvents()
                    .skip(1)
                    .share()
            val writingObservable = searchEventsObservable
                    .filter { !it.isSubmitted }
                    .debounce(1250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            val submitObservable = searchEventsObservable
                    .filter { it.isSubmitted }

            searchViewSubscription?.unsubscribe()
            searchViewSubscription = Observable.merge(writingObservable, submitObservable)
                    .map { it.queryText().toString() }
                    .distinctUntilChanged()
                    .subscribeUntilDestroy { searchWithQuery(it) }

            untilDestroySubscriptions.add(
                    Subscriptions.create { if (isActionViewExpanded) collapseActionView() })
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
        adapter?.clear()

        presenter.restartPager(newQuery)
    }

    /**
     * Called from the presenter when the network request is received.
     *
     * @param page the current page.
     * @param mangas the list of manga of the page.
     */
    fun onAddPage(page: Int, mangas: List<CatalogueItem>) {
        val adapter = adapter ?: return
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
        Timber.e(error)
        val adapter = adapter ?: return
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
        adapter?.endlessTargetCount = 0
        adapter?.setEndlessScrollListener(this, progressItem!!)
    }

    /**
     * Called by the adapter when scrolled near the bottom.
     */
    override fun onLoadMore(lastPosition: Int, currentPage: Int) {
        Timber.e("onLoadMore")
        if (presenter.hasNextPage()) {
            presenter.requestNext()
        } else {
            adapter?.onLoadMoreComplete(null)
            adapter?.endlessTargetCount = 1
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
        val adapter = adapter ?: return

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
        val adapter = adapter ?: return null

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
        view?.progress?.visible()
        snack?.dismiss()
        snack = null
    }

    /**
     * Hides active progress bars.
     */
    private fun hideProgressBar() {
        view?.progress?.gone()
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onItemClick(position: Int): Boolean {
        val item = adapter?.getItem(position) as? CatalogueItem ?: return false
        router.pushController(RouterTransaction.with(MangaController(item.manga, true))
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
        val manga = (adapter?.getItem(position) as? CatalogueItem?)?.manga ?: return
        if (manga.favorite) {
            MaterialDialog.Builder(activity!!)
                    .items(resources?.getString(R.string.remove_from_library))
                    .itemsCallback { _, _, which, _ ->
                        when (which) {
                            0 -> {
                                presenter.changeMangaFavorite(manga)
                                adapter?.notifyItemChanged(position)
                            }
                        }
                    }.show()
        } else {
            presenter.changeMangaFavorite(manga)
            adapter?.notifyItemChanged(position)

            val categories = presenter.getCategories()
            val defaultCategory = categories.find { it.id == preferences.defaultCategory() }
            if (defaultCategory != null) {
                presenter.moveMangaToCategory(manga, defaultCategory)
            } else if (categories.size <= 1) { // default or the one from the user
                presenter.moveMangaToCategory(manga, categories.firstOrNull())
            } else {
                val ids = presenter.getMangaCategoryIds(manga)
                val preselected = ids.mapNotNull { id ->
                    categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
                }.toTypedArray()

                ChangeMangaCategoriesDialog(this, listOf(manga), categories, preselected)
                        .showDialog(router)
            }
        }

    }

    /**
     * Update manga to use selected categories.
     *
     * @param mangas The list of manga to move to categories.
     * @param categories The list of categories where manga will be placed.
     */
    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        val manga = mangas.firstOrNull() ?: return
        presenter.updateMangaCategories(manga, categories)
    }

}
