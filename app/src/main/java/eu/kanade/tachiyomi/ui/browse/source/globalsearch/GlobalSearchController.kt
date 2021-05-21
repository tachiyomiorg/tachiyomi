package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.GlobalSearchControllerBinding
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.base.controller.SearchableNucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.manga.MangaController
import uy.kohesive.injekt.injectLazy

/**
 * This controller shows and manages the different search result in global search.
 * This controller should only handle UI actions, IO actions should be done by [GlobalSearchPresenter]
 * [GlobalSearchCardAdapter.OnMangaClickListener] called when manga is clicked in global search
 */
open class GlobalSearchController(
    protected val initialQuery: String? = null,
    protected val extensionFilter: String? = null
) : SearchableNucleusController<GlobalSearchControllerBinding, GlobalSearchPresenter>(),
    GlobalSearchCardAdapter.OnMangaClickListener,
    GlobalSearchAdapter.OnTitleClickListener {

    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Adapter containing search results grouped by lang.
     */
    protected var adapter: GlobalSearchAdapter? = null

    /**
     * Ref to the OptionsMenu.SearchItem created in onCreateOptionsMenu
     */
    private var optionsMenuSearchItem: MenuItem? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun createBinding(inflater: LayoutInflater) = GlobalSearchControllerBinding.inflate(inflater)

    override fun getTitle(): String? {
        return presenter.query
    }

    /**
     * Create the [GlobalSearchPresenter] used in controller.
     *
     * @return instance of [GlobalSearchPresenter]
     */
    override fun createPresenter(): GlobalSearchPresenter {
        return GlobalSearchPresenter(initialQuery, extensionFilter)
    }

    /**
     * Called when manga in global search is clicked, opens manga.
     *
     * @param manga clicked item containing manga information.
     */
    override fun onMangaClick(manga: Manga) {
        router.pushController(MangaController(manga, true).withFadeTransaction())
    }

    /**
     * Called when manga in global search is long clicked.
     *
     * @param manga clicked item containing manga information.
     */
    override fun onMangaLongClick(manga: Manga) {
        // Delegate to single click by default.
        onMangaClick(manga)
    }

    /**
     * Adds items to the options menu.
     *
     * @param menu menu containing options.
     * @param inflater used to load the menu xml.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        createOptionsMenu(
            menu,
            inflater,
            R.menu.global_search,
            R.id.action_search,
            null,
            false // the onMenuItemActionExpand will handle this
        )

        optionsMenuSearchItem = menu.findItem(R.id.action_search)
    }

    override fun onSearchMenuItemActionExpand(item: MenuItem?) {
        super.onSearchMenuItemActionExpand(item)
        val searchView = optionsMenuSearchItem?.actionView as SearchView
        searchView.onActionViewExpanded() // Required to show the query in the view

        if (nonSubmittedQuery.isBlank()) {
            searchView.setQuery(presenter.query, false)
        }
    }

    override fun onSearchViewQueryTextSubmit(query: String?) {
        presenter.search(query ?: "")
        optionsMenuSearchItem?.collapseActionView()
        setTitle() // Update toolbar title
    }

    /**
     * Called when the view is created
     *
     * @param view view of controller
     */
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        adapter = GlobalSearchAdapter(this)

        // Create recycler and set adapter.
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        adapter?.onSaveInstanceState(outState)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        adapter?.onRestoreInstanceState(savedViewState)
    }

    /**
     * Returns the view holder for the given manga.
     *
     * @param source used to find holder containing source
     * @return the holder of the manga or null if it's not bound.
     */
    private fun getHolder(source: CatalogueSource): GlobalSearchHolder? {
        val adapter = adapter ?: return null

        adapter.allBoundViewHolders.forEach { holder ->
            val item = adapter.getItem(holder.bindingAdapterPosition)
            if (item != null && source.id == item.source.id) {
                return holder as GlobalSearchHolder
            }
        }

        return null
    }

    /**
     * Add search result to adapter.
     *
     * @param searchResult result of search.
     */
    fun setItems(searchResult: List<GlobalSearchItem>) {
        if (searchResult.isEmpty() && preferences.searchPinnedSourcesOnly()) {
            binding.emptyView.show(R.string.no_pinned_sources)
        } else {
            binding.emptyView.hide()
        }

        adapter?.updateDataSet(searchResult)

        val progress = searchResult.mapNotNull { it.results }.size.toDouble() / searchResult.size
        if (progress < 1) {
            binding.progressBar.isVisible = true
            binding.progressBar.progress = (progress * 100).toInt()
        } else {
            binding.progressBar.isVisible = false
        }
    }

    /**
     * Called from the presenter when a manga is initialized.
     *
     * @param manga the initialized manga.
     */
    fun onMangaInitialized(source: CatalogueSource, manga: Manga) {
        getHolder(source)?.setImage(manga)
    }

    /**
     * Opens a catalogue with the given search.
     */
    override fun onTitleClick(source: CatalogueSource) {
        presenter.preferences.lastUsedSource().set(source.id)
        router.pushController(BrowseSourceController(source, presenter.query).withFadeTransaction())
    }
}
