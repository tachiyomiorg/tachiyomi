package eu.kanade.tachiyomi.ui.setting.database

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DatabaseSourcesControllerBinding
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.FabController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.setTooltip
import eu.kanade.tachiyomi.util.view.shrinkOnScroll

class DatabaseSourcesController :
    NucleusController<DatabaseSourcesControllerBinding, DatabaseSourcesPresenter>(),
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnUpdateListener,
    FabController {

    private var recycler: RecyclerView? = null

    private var adapter: DatabaseSourceAdapter? = null

    private var actionFab: ExtendedFloatingActionButton? = null

    private var actionFabScrollListener: RecyclerView.OnScrollListener? = null

    init {
        setHasOptionsMenu(true)
    }

    val selectedSources: MutableSet<Source> = mutableSetOf()

    override fun getTitle(): String {
        return activity!!.getString(R.string.pref_clear_database)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        adapter = DatabaseSourceAdapter(this)
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter
        actionFabScrollListener = binding.recycler.let { actionFab?.shrinkOnScroll(it) }
        recycler = binding.recycler

        adapter?.fastScroller = binding.fastScroller
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun createBinding(inflater: LayoutInflater): DatabaseSourcesControllerBinding {
        return DatabaseSourcesControllerBinding.inflate(inflater)
    }

    override fun createPresenter(): DatabaseSourcesPresenter {
        return DatabaseSourcesPresenter()
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        toggleSelection(position)
        adapter!!.notifyItemChanged(position)
        return false
    }

    override fun configureFab(fab: ExtendedFloatingActionButton) {
        fab.setIconResource(R.drawable.ic_delete_24dp)
        fab.setText(R.string.action_delete)
        fab.isVisible = false
        fab.setTooltip(R.string.action_delete)
        fab.setOnClickListener {
            val ctrl = ClearDatabaseSourcesDialog()
            ctrl.targetController = this
            ctrl.showDialog(router)
        }
        actionFab = fab
    }

    override fun cleanupFab(fab: ExtendedFloatingActionButton) {
        actionFab?.setOnClickListener(null)
        actionFabScrollListener?.let { recycler?.removeOnScrollListener(it) }
        actionFab = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.generic_selection, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val adapter = adapter ?: return super.onOptionsItemSelected(item)

        when (item.itemId) {
            R.id.action_select_all -> {
                for (i in 0..adapter.itemCount) {
                    addSelection(i)
                }
            }
            R.id.action_select_inverse -> {
                for (i in 0..adapter.itemCount) {
                    toggleSelection(i)
                }
            }
        }

        adapter.notifyDataSetChanged()
        return super.onOptionsItemSelected(item)
    }

    override fun onUpdateEmptyView(size: Int) {
        if (size > 0) {
            binding.emptyView.hide()
        } else {
            binding.emptyView.show(R.string.database_clean_message)
        }
    }

    fun setDatabaseSources(sources: List<DatabaseSourceItem>) {
        adapter?.updateDataSet(sources)
    }

    fun addSelection(position: Int) {
        val adapter = adapter ?: return
        val source = adapter.getItem(position)?.source ?: return
        adapter.addSelection(position)
        selectedSources.add(source)
        actionFab!!.isVisible = true
    }

    private fun toggleSelection(position: Int) {
        val adapter = adapter ?: return
        val source = adapter.getItem(position)?.source ?: return
        adapter.toggleSelection(position)
        if (adapter.isSelected(position)) {
            selectedSources.add(source)
        } else {
            selectedSources.remove(source)
        }

        actionFab!!.isVisible = selectedSources.size > 0
    }

    class ClearDatabaseSourcesDialog : DialogController() {

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            return MaterialDialog(activity!!)
                .message(R.string.clear_database_confirmation)
                .positiveButton(android.R.string.ok) {
                    (targetController as? DatabaseSourcesController)?.clearDatabaseForSources()
                }
                .negativeButton(android.R.string.cancel)
        }
    }

    private fun clearDatabaseForSources() {
        presenter.clearDatabaseForSources(selectedSources.map { it.id })
        selectedSources.clear()
        actionFab!!.isVisible = false
        adapter?.clearSelection()
        adapter?.notifyDataSetChanged()
        activity?.toast(R.string.clear_database_completed)
    }
}
