package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.R.layout.catalogue_search_controller
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import kotlinx.android.synthetic.main.catalogue_search_controller.view.*

class CatalogueSearchController(val query:  String = "") : NucleusController<CatalogueSearchPresenter>() {

    private var adapter: CatalogueSearchAdapter? = null

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): android.view.View {
        return inflater.inflate(catalogue_search_controller, container, false)
    }

    init {
        setHasOptionsMenu(true)
        presenter.globalSource(query)
    }

    override fun getTitle(): String? {
        return applicationContext?.getString(R.string.action_global_search)
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
     * Returns the view holder for the given manga.
     *
     * @param manga the manga to find.
     * @return the holder of the manga or null if it's not bound.
     */
    private fun getHolder(manga: Manga): CatalogueSearchHolder? {
        val adapter = adapter ?: return null

        adapter.allBoundViewHolders.forEach { holder ->
            val item = adapter.getItem(holder.adapterPosition)
            if (item != null && manga in item.searchResult.first) {
                return holder as CatalogueSearchHolder
            }
        }

        return null
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)
        with(view) {
            adapter = CatalogueSearchAdapter(this@CatalogueSearchController)
            recycler.layoutManager = LinearLayoutManager(context)
            recycler.setHasFixedSize(true)
            recycler.adapter = adapter
            adapter?.isHandleDragEnabled = true
        }
    }

    /**
     * Adds option items to the host Activity's standard options menu. This will only be called if
     * [.setHasOptionsMenu] has been called.

     * @param menu The menu into which your options should be placed.
     * *
     * @param inflater The inflater that can be used to inflate your menu items.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.catalogue_new_list, menu)

        // Initialize search menu
        menu.findItem(R.id.action_search).apply {
            val searchView = actionView as SearchView

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    if (query != presenter.query){
                        adapter?.clear()
                        presenter.globalSource(query)
                    }
                    collapseActionView()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    // TODO suggestions?
                    return true
                }
            })
        }
    }

    override fun createPresenter(): CatalogueSearchPresenter {
        return CatalogueSearchPresenter()
    }

    fun updateList(searchResult: CatalogueSearchItem) {
        adapter?.addItem(searchResult)
    }

}