package eu.kanade.tachiyomi.ui.browse.source

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.minusAssign
import eu.kanade.tachiyomi.data.preference.plusAssign
import eu.kanade.tachiyomi.databinding.SourceMainControllerBinding
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.SearchableNucleusController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.BrowseController
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchController
import eu.kanade.tachiyomi.ui.browse.source.latest.LatestUpdatesController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This controller shows and manages the different catalogues enabled by the user.
 * This controller should only handle UI actions, IO actions should be done by [SourcePresenter]
 * [SourceAdapter.OnSourceClickListener] call function data on browse item click.
 * [SourceAdapter.OnLatestClickListener] call function data on latest item click
 */
class SourceController :
    SearchableNucleusController<SourceMainControllerBinding, SourcePresenter>(),
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    SourceAdapter.OnSourceClickListener {

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

    override fun createBinding(inflater: LayoutInflater) = SourceMainControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        adapter = SourceAdapter(this)

        // Create recycler and set adapter.
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter
        adapter?.fastScroller = binding.fastScroller

        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 301)

        // Update list on extension changes (e.g. new installation)
        (parentController as BrowseController).extensionListUpdateRelay
            .subscribeUntilDestroy {
                presenter.updateSources()
            }
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
        onItemClick(position)
        return false
    }

    private fun onItemClick(position: Int) {
        val item = adapter?.getItem(position) as? SourceItem ?: return
        val source = item.source
        openSource(source, BrowseSourceController(source))
    }

    override fun onItemLongClick(position: Int) {
        val activity = activity ?: return
        val item = adapter?.getItem(position) as? SourceItem ?: return

        val isPinned = item.header?.code?.equals(SourcePresenter.PINNED_KEY) ?: false

        val items = mutableListOf(
            Pair(
                activity.getString(if (isPinned) R.string.action_unpin else R.string.action_pin),
                { toggleSourcePin(item.source) }
            )
        )
        if (item.source !is LocalSource) {
            items.add(
                Pair(
                    activity.getString(R.string.action_disable),
                    { disableSource(item.source) }
                )
            )
        }

        SourceOptionsDialog(item.source.toString(), items).showDialog(router)
    }

    private fun disableSource(source: Source) {
        preferences.disabledSources() += source.id.toString()

        presenter.updateSources()
    }

    private fun toggleSourcePin(source: Source) {
        val isPinned = source.id.toString() in preferences.pinnedSources().get()
        if (isPinned) {
            preferences.pinnedSources() -= source.id.toString()
        } else {
            preferences.pinnedSources() += source.id.toString()
        }

        presenter.updateSources()
    }

    /**
     * Called when browse is clicked in [SourceAdapter]
     */
    override fun onBrowseClick(position: Int) {
        onItemClick(position)
    }

    /**
     * Called when latest is clicked in [SourceAdapter]
     */
    override fun onLatestClick(position: Int) {
        val item = adapter?.getItem(position) as? SourceItem ?: return
        openSource(item.source, LatestUpdatesController(item.source))
    }

    /**
     * Called when pin icon is clicked in [SourceAdapter]
     */
    override fun onPinClick(position: Int) {
        val item = adapter?.getItem(position) as? SourceItem ?: return
        toggleSourcePin(item.source)
    }

    /**
     * Opens a catalogue with the given controller.
     */
    private fun openSource(source: CatalogueSource, controller: BrowseSourceController) {
        if (!preferences.incognitoMode().get()) {
            preferences.lastUsedSource().set(source.id)
        }
        parentController!!.router.pushController(controller.withFadeTransaction())
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
                parentController!!.router.pushController(
                    SourceFilterController()
                        .withFadeTransaction()
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

    class SourceOptionsDialog(bundle: Bundle? = null) : DialogController(bundle) {

        private lateinit var source: String
        private lateinit var items: List<Pair<String, () -> Unit>>

        constructor(source: String, items: List<Pair<String, () -> Unit>>) : this() {
            this.source = source
            this.items = items
        }

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            return MaterialDialog(activity!!)
                .title(text = source)
                .listItems(
                    items = items.map { it.first },
                    waitForPositiveButton = false
                ) { dialog, which, _ ->
                    items[which].second()
                    dialog.dismiss()
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        createOptionsMenu(
            menu,
            inflater,
            R.menu.source_main,
            R.id.action_search,
            R.string.action_global_search_hint,
            false // GlobalSearch handles the searching here
        )
    }

    override fun onSearchViewQueryTextSubmit(query: String?) {
        parentController!!.router.pushController(
            GlobalSearchController(query).withFadeTransaction()
        )
    }
}
