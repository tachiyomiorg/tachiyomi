package eu.kanade.tachiyomi.ui.catalogue_new

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.catalogue.CatalogueController
import eu.kanade.tachiyomi.ui.catalogue_new.search_result.CatalogueSearchController
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import kotlinx.android.synthetic.main.catalogue_new_controller.view.*

/**
 * This controller handles the different sources available to the user.
 * It will show the sources fetched from [CataloguePresenter].
 * OnItemClickListener: Opens selected [CatalogueSource] from [CatalogueAdapter].
 */
class CatalogueController : NucleusController<CataloguePresenter>() {

    /**
     * Adapter containing most recent used source //TODO increase number of recent?
     */
    private var adapterRecent: CatalogueAdapter? = null

    /**
     * Adapter containing sources
     */
    private var adapter: CatalogueAdapter? = null

    /**
     * Adapter containing favorite sources TODO implement in settings
     */
    private var adapterFavorite: CatalogueAdapter? = null

    /**
     * Global search query
     */
    var query: String = ""
        private set

    init {
        // Enable the option menu
        setHasOptionsMenu(true)
    }

    /**
     * Inflate the view with catalogue_controller layout.
     *
     * @param inflater Used to instantiates XML layout.
     * @param container Container of different views.
     */
    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.catalogue_new_controller, container, false)
    }

    override fun getTitle(): String? {
        return applicationContext?.getString(R.string.label_catalogues)
    }

    /**
     * Create the [CataloguePresenter] used in controller.
     */
    override fun createPresenter(): CataloguePresenter {
        return CataloguePresenter()
    }

    /**
     * Called when user clicks item in [recycler]
     *
     * @param position Selected item position in [adapter].
     */
    fun onItemClick(position: Int, favorite: Boolean, recent: Boolean = false): Boolean {
        // Get item from position
        val item: CatalogueItem
        if (recent) {
            item = adapterRecent?.getItem(position) as? CatalogueItem ?: return false
        } else {
            if (favorite)
                item = adapterFavorite?.getItem(position) as? CatalogueItem ?: return false
            else
                item = adapter?.getItem(position) as? CatalogueItem ?: return false
        }

        val source = item.source
        if (source is LoginSource && !source.isLogged()) {
            val dialog = SourceLoginDialog(source)
            dialog.targetController = this@CatalogueController
            dialog.showDialog(router)
        }else {
            // Update last used
            presenter.setLastUsedSource(item.source.id)
            // Open the catalogue view.
            router.pushController(RouterTransaction.with(CatalogueController(null, item.source as CatalogueSource))
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler()))
        }
        return false
    }


    /**
     * Called when the view is created
     *
     * @param view Containing view information
     * @param savedViewState Information from previous state.
     */
    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        with(view) {
            // Add the sources to the adapter
            adapter = CatalogueAdapter(FlexibleAdapter.OnItemClickListener {
                onItemClick(it, false)
            })
            adapterFavorite = CatalogueAdapter(FlexibleAdapter.OnItemClickListener {
                onItemClick(it, true)
            })

            adapterRecent = CatalogueAdapter(FlexibleAdapter.OnItemClickListener {
                onItemClick(it, false, true)
            })

            // Create recycler
            recycler.layoutManager = LinearLayoutManager(context)
            val dividerItemDecoration = DividerItemDecoration(recycler.context,
                    (recycler.layoutManager as LinearLayoutManager).orientation)
            recycler.addItemDecoration(dividerItemDecoration)
            recycler.setHasFixedSize(true)
            recycler.adapter = adapter

            recycler_favorite.layoutManager = LinearLayoutManager(context)
            recycler_favorite.setHasFixedSize(true)
            recycler_favorite.adapter = adapterFavorite

            recycler_recent.layoutManager = LinearLayoutManager(context)
            recycler_recent.setHasFixedSize(true)
            recycler_recent.adapter = adapterRecent

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

    fun setSources(sources: List<CatalogueItem>) {
        adapter?.updateDataSet(sources)
    }

    fun setLastUsedSource(source: Source?) {
        if (source != null) {
            val sources = listOf(source).map(::CatalogueItem)
            adapterRecent?.updateDataSet(sources)
            adapterRecent?.notifyDataSetChanged()
        }
        else
            return //TODO error
    }

    fun setSourcesFavorite(sources: List<CatalogueItem>) {
        if (sources.isEmpty()) {
            //TODO show empty warning
        } else {
            // Show favorite catalogues
            adapterFavorite?.updateDataSet(sources)
            adapterFavorite?.notifyDataSetChanged()
        }
    }
}