package eu.kanade.tachiyomi.ui.catalogue.main

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchController
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import kotlinx.android.synthetic.main.catalogue_main_controller.view.*

class CatalogueMainController : NucleusController<CatalogueMainPresenter>(), SourceLoginDialog.Listener {
    /**
     * Adapter containing sources
     */
    private var adapter = CatalogueMainAdapter(this)

    /**
     *
     */
    var firstUse = true

    /**
     *
     */
    var recentCatalogue: CatalogueMainItem? = null

    init {
        // Enable the option menu
        setHasOptionsMenu(true)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.catalogue_main_controller, container, false)
    }

    override fun getTitle(): String? {
        return applicationContext?.getString(R.string.label_catalogues)
    }

    override fun createPresenter(): CatalogueMainPresenter {
        return CatalogueMainPresenter()
    }

    override fun loginDialogClosed(source: LoginSource) {
        adapter.clear()
        firstUse = true
        presenter.loadSources()
        presenter.loadRecentSources()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.catalogue_main, menu)

        // Initialize search menu
        menu.findItem(R.id.action_search).apply {
            val searchView = actionView as SearchView

            searchView.queryHint = applicationContext?.getString(R.string.action_global_search_hint)

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    router.pushController((RouterTransaction.with(CatalogueSearchController(query)))
                            .popChangeHandler(FadeChangeHandler())
                            .pushChangeHandler(FadeChangeHandler()))
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    // TODO suggestions?
                    return true
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)
        with(view) {
            // Create recycler
            recycler.layoutManager = LinearLayoutManager(context)
            recycler.setHasFixedSize(true)
            recycler.adapter = adapter
        }
    }

    fun setSources(sources: List<CatalogueMainItem>) {
        adapter.updateDataSet(sources)
    }

    fun setLastUsedSource(source: CatalogueMainItem) {
        recentCatalogue = source
        recentCatalogue?.let {
            if (firstUse) {
                adapter.addItem(0, it)
                firstUse = false
            }else{
                adapter.updateItem(0,it,null)
            }
        }

    }
}