package eu.kanade.tachiyomi.ui.catalogue.main

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.catalogue.CatalogueController
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchController
import eu.kanade.tachiyomi.ui.latest_updates.LatestUpdatesController
import eu.kanade.tachiyomi.ui.setting.SettingsSourcesController
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import kotlinx.android.synthetic.main.catalogue_main_controller.view.*

/**
 * This controller shows and manages the different catalogues enabled by the user.
 * This controller should only handle UI actions, IO actions should be done by [CatalogueMainPresenter]
 * [SourceLoginDialog.Listener] refreshes the adapter on successful login of catalogues.
 * [CatalogueMainCardAdapter.OnBrowseClickListener] call function data on browse item click.
 * [CatalogueMainCardAdapter.OnLatestClickListener] call function data on latest item click
 */
class CatalogueMainController : NucleusController<CatalogueMainPresenter>(),
        SourceLoginDialog.Listener,
        CatalogueMainCardAdapter.OnBrowseClickListener,
        CatalogueMainCardAdapter.OnLatestClickListener {
    /**
     * Adapter containing sources.
     */
    private var adapter : CatalogueMainAdapter? = null

    /**
     * Called when controller is initialized.
     */
    init {
        // Enable the option menu
        setHasOptionsMenu(true)
    }


    /**
     * Initiate the view with [R.layout.catalogue_main_controller].
     *
     * @param inflater used to load the layout xml.
     * @param container containing parent views.
     * @return inflated view.
     */
    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.catalogue_main_controller, container, false)
    }

    /**
     * Set  the title of controller.
     *
     * @return title.
     */
    override fun getTitle(): String? {
        return applicationContext?.getString(R.string.label_catalogues)
    }

    /**
     * Create the [CatalogueMainPresenter] used in controller.
     *
     * @return instance of [CatalogueMainPresenter]
     */
    override fun createPresenter(): CatalogueMainPresenter {
        return CatalogueMainPresenter()
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (!type.isPush && handler is SettingsSourceFadeChangeHandler) {
            presenter.updateSources()
        }
    }

    /**
     * Called when login dialog is closed, refreshes the adapter.
     *
     * @param source clicked item containing source information.
     */
    override fun loginDialogClosed(source: LoginSource) {
        if (source.isLogged()){
            adapter?.clear()
            presenter.loadSources()
        }
    }

    /**
     * Called when browse or item is clicked in [CatalogueMainCardAdapter]
     */
    override fun onBrowseClickListener(item: CatalogueMainCardItem) {
        val source = item.source
        if (source is LoginSource && !source.isLogged()) {
            val dialog = SourceLoginDialog(source)
            dialog.targetController = this
            dialog.showDialog(router)
        } else {
            // Open the catalogue view.
            router.pushController(RouterTransaction.with(CatalogueController(null, item.source))
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler()))
        }
    }

    /**
     * Called when latest is clicked in [CatalogueMainCardAdapter]
     */
    override fun OnLatestClickListener(item: CatalogueMainCardItem) {
        router.pushController((RouterTransaction.with(LatestUpdatesController(null, item.source)))
                .popChangeHandler(FadeChangeHandler())
                .pushChangeHandler(FadeChangeHandler()))
    }

    /**
     * Adds items to the options menu.
     *
     * @param menu menu containing options.
     * @param inflater used to load the menu xml.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate menu
        inflater.inflate(R.menu.catalogue_main, menu)

        // Initialize search option.
        menu.findItem(R.id.action_search).apply {
            val searchView = actionView as SearchView

            // Change hint to show global search.
            searchView.queryHint = applicationContext?.getString(R.string.action_global_search_hint)

            // Create query listener which opens the global search view.s
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

    /**
     * Called when an option menu item has been selected by the user.
     *
     * @param item The selected item.
     * @return True if this event has been consumed, false if it has not.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Initialize option to open catalogue settings.
            R.id.action_settings -> {
                router.pushController((RouterTransaction.with(SettingsSourcesController()))
                        .popChangeHandler(SettingsSourceFadeChangeHandler())
                        .pushChangeHandler(FadeChangeHandler()))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Called when the view is created
     *
     * @param view view of controller
     * @param savedViewState information from previous state.
     */
    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        adapter = CatalogueMainAdapter(this)

        with(view) {
            // Create recycler and set adapter.
            recycler.layoutManager = LinearLayoutManager(context)
            recycler.isNestedScrollingEnabled = false
            recycler.adapter = adapter
        }
    }

    override fun onDestroyView(view: View) {
        adapter = null

        super.onDestroyView(view)
    }

    /**
     * Called to update adapter containing sources.
     */
    fun setSources(sources: List<CatalogueMainItem>) {
        adapter?.updateDataSet(sources)
    }

    private class SettingsSourceFadeChangeHandler : FadeChangeHandler()
}