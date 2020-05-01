package eu.kanade.tachiyomi.ui.browse.source

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.SourceMainControllerBinding
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.BrowseController
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchController
import eu.kanade.tachiyomi.ui.browse.source.latest.LatestUpdatesController
import eu.kanade.tachiyomi.ui.setting.SettingsSourcesController
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.QueryTextEvent
import reactivecircus.flowbinding.appcompat.queryTextEvents
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This controller shows and manages the different catalogues enabled by the user.
 * This controller should only handle UI actions, IO actions should be done by [SourcePresenter]
 * [SourceAdapter.OnBrowseClickListener] call function data on browse item click.
 * [SourceAdapter.OnLatestClickListener] call function data on latest item click
 */
class SourceController :
    NucleusController<SourceMainControllerBinding, SourcePresenter>(),
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    SourceAdapter.OnBrowseClickListener,
    SourceAdapter.OnLatestClickListener {

    private val preferences: PreferencesHelper = Injekt.get()

    /**
     * Adapter containing sources.
     */
    private var adapter: SourceAdapter? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return applicationContext?.getString(R.string.label_sources)
    }

    override fun createPresenter(): SourcePresenter {
        return SourcePresenter()
    }

    /**
     * Initiate the view with [R.layout.source_main_controller].
     *
     * @param inflater used to load the layout xml.
     * @param container containing parent views.
     * @return inflated view.
     */
    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = SourceMainControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = SourceAdapter(this)

        // Create recycler and set adapter.
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter
        binding.recycler.addItemDecoration(SourceDividerItemDecoration(view.context))
        adapter?.fastScroller = binding.fastScroller

        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 301)
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isPush) {
            presenter.updateSources()
        }
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter?.getItem(position) as? SourceItem ?: return false
        val source = item.source
        openCatalogue(source, BrowseSourceController(source))
        return false
    }

    override fun onItemLongClick(position: Int) {
        val activity = activity ?: return
        val item = adapter?.getItem(position) as? SourceItem ?: return

        val isPinned = item.header?.code?.equals(SourcePresenter.PINNED_KEY) ?: false

        MaterialDialog(activity)
            .title(text = item.source.name)
            .listItems(
                items = listOf(
                    activity.getString(R.string.action_hide),
                    activity.getString(if (isPinned) R.string.action_unpin else R.string.action_pin)
                ),
                waitForPositiveButton = false
            ) { dialog, which, _ ->
                when (which) {
                    0 -> hideCatalogue(item.source)
                    1 -> pinCatalogue(item.source, isPinned)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun hideCatalogue(source: Source) {
        val current = preferences.hiddenCatalogues().get()
        preferences.hiddenCatalogues().set(current + source.id.toString())

        presenter.updateSources()
    }

    private fun pinCatalogue(source: Source, isPinned: Boolean) {
        val current = preferences.pinnedCatalogues().get()
        if (isPinned) {
            preferences.pinnedCatalogues().set(current - source.id.toString())
        } else {
            preferences.pinnedCatalogues().set(current + source.id.toString())
        }

        presenter.updateSources()
    }

    /**
     * Called when browse is clicked in [SourceAdapter]
     */
    override fun onBrowseClick(position: Int) {
        onItemClick(view!!, position)
    }

    /**
     * Called when latest is clicked in [SourceAdapter]
     */
    override fun onLatestClick(position: Int) {
        val item = adapter?.getItem(position) as? SourceItem ?: return
        openCatalogue(item.source, LatestUpdatesController(item.source))
    }

    /**
     * Opens a catalogue with the given controller.
     */
    private fun openCatalogue(source: CatalogueSource, controller: BrowseSourceController) {
        preferences.lastUsedCatalogueSource().set(source.id)
        (parentController as BrowseController).pushController(controller.withFadeTransaction())
    }

    /**
     * Adds items to the options menu.
     *
     * @param menu menu containing options.
     * @param inflater used to load the menu xml.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate menu
        inflater.inflate(R.menu.source_main, menu)

        // Initialize search option.
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE

        // Change hint to show global search.
        searchView.queryHint = applicationContext?.getString(R.string.action_global_search_hint)

        // Create query listener which opens the global search view.
        searchView.queryTextEvents()
            .filter { it is QueryTextEvent.QuerySubmitted }
            .onEach { performGlobalSearch(it.queryText.toString()) }
            .launchIn(scope)
    }

    private fun performGlobalSearch(query: String) {
        (parentController as BrowseController).pushController(
            GlobalSearchController(query).withFadeTransaction()
        )
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
                (parentController as BrowseController).pushController(
                    SettingsSourcesController().withFadeTransaction()
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Called to update adapter containing sources.
     */
    fun setSources(sources: List<IFlexible<*>>) {
        adapter?.updateDataSet(sources)
    }

    /**
     * Called to set the last used catalogue at the top of the view.
     */
    fun setLastUsedSource(item: SourceItem?) {
        adapter?.removeAllScrollableHeaders()
        if (item != null) {
            adapter?.addScrollableHeader(item)
            adapter?.addScrollableHeader(LangItem(SourcePresenter.LAST_USED_KEY))
        }
    }
}
