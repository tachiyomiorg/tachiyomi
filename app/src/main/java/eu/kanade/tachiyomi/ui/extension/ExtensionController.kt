package eu.kanade.tachiyomi.ui.extension

import android.app.SearchManager
import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFilterable
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import kotlinx.android.synthetic.main.extension_controller.*


/**
 * Controller to manage the catalogues available in the app.
 */
open class ExtensionController : NucleusController<ExtensionPresenter>(),
        ExtensionAdapter.OnButtonClickListener,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        ExtensionTrustDialog.Listener {


    /**
     * Adapter containing the list of manga from the catalogue.
     */
    private var adapter: FlexibleAdapter<IFlexible<*>>? = null
    private val allExtensions: MutableList<IFlexible<*>> = mutableListOf()
    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return applicationContext?.getString(R.string.label_extensions)
    }

    override fun createPresenter(): ExtensionPresenter {
        return ExtensionPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.extension_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        ext_swipe_refresh.isRefreshing = true
        ext_swipe_refresh.refreshes().subscribeUntilDestroy {
            presenter.findAvailableExtensions()
        }

        // Initialize adapter, scroll listener and recycler views
        adapter = ExtensionAdapter(this)
        // Create recycler and set adapter.
        ext_recycler.layoutManager = LinearLayoutManager(view.context)
        ext_recycler.adapter = adapter
        ext_recycler.addItemDecoration(ExtensionDividerItemDecoration(view.context))
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onButtonClick(position: Int) {
        val extension = (adapter?.getItem(position) as? ExtensionItem)?.extension ?: return
        when (extension) {
            is Extension.Installed -> {
                if (!extension.hasUpdate) {
                    openDetails(extension)
                } else {
                    presenter.updateExtension(extension)
                }
            }
            is Extension.Available -> {
                presenter.installExtension(extension)
            }
            is Extension.Untrusted -> {
                openTrustDialog(extension)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.ext_menu, menu)

        val searchManager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager

        val searchView = menu.findItem(R.id.action_search)
                .actionView as SearchView
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(activity?.componentName))
        searchView.maxWidth = Integer.MAX_VALUE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                return onQueryTextChange(query)
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter?.searchText= newText!!
                adapter?.filterItems(allExtensions)
                return false
            }
        })
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.action_search) {
            true
        } else super.onOptionsItemSelected(item)

    }
    override fun onItemClick(position: Int): Boolean {
        val extension = (adapter?.getItem(position) as? ExtensionItem)?.extension ?: return false
        if (extension is Extension.Installed) {
            openDetails(extension)
        } else if (extension is Extension.Untrusted) {
            openTrustDialog(extension)
        }

        return false
    }

    override fun onItemLongClick(position: Int) {
        val extension = (adapter?.getItem(position) as? ExtensionItem)?.extension ?: return
        if (extension is Extension.Installed || extension is Extension.Untrusted) {
            uninstallExtension(extension.pkgName)
        }
    }

    private fun openDetails(extension: Extension.Installed) {
        val controller = ExtensionDetailsController(extension.pkgName)
        router.pushController(controller.withFadeTransaction())
    }

    private fun openTrustDialog(extension: Extension.Untrusted) {
        ExtensionTrustDialog(this, extension.signatureHash, extension.pkgName)
                .showDialog(router)
    }

    fun setExtensions(extensions: List<ExtensionItem>) {
        ext_swipe_refresh?.isRefreshing = false
        allExtensions.clear()
        allExtensions.addAll(extensions)
        adapter?.updateDataSet(extensions)

    }

    fun downloadUpdate(item: ExtensionItem) {
        adapter?.updateItem(item, item.installStep)
    }

    override fun trustSignature(signatureHash: String) {
        presenter.trustSignature(signatureHash)
    }

    override fun uninstallExtension(pkgName: String) {
        presenter.uninstallExtension(pkgName)
    }

}
