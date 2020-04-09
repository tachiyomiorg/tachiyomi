package eu.kanade.tachiyomi.ui.migration

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MigrationControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction

class MigrationController : NucleusController<MigrationPresenter>(),
        FlexibleAdapter.OnItemClickListener,
        SourceAdapter.OnSelectClickListener {

    private var adapter: FlexibleAdapter<IFlexible<*>>? = null

    private var title: String? = null
        set(value) {
            field = value
            setTitle()
        }

    private lateinit var binding: MigrationControllerBinding

    override fun createPresenter(): MigrationPresenter {
        return MigrationPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = MigrationControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = FlexibleAdapter(null, this)
        binding.migrationRecycler.layoutManager = LinearLayoutManager(view.context)
        binding.migrationRecycler.adapter = adapter
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun getTitle(): String? {
        return title
    }

    override fun handleBack(): Boolean {
        return if (presenter.state.selectedSource != null) {
            presenter.deselectSource()
            true
        } else {
            super.handleBack()
        }
    }

    fun render(state: ViewState) {
        if (state.selectedSource == null) {
            title = resources?.getString(R.string.label_migration)
            if (adapter !is SourceAdapter) {
                adapter = SourceAdapter(this)
                binding.migrationRecycler.adapter = adapter
            }
            adapter?.updateDataSet(state.sourcesWithManga)
        } else {
            title = state.selectedSource.toString()
            if (adapter !is MangaAdapter) {
                adapter = MangaAdapter(this)
                binding.migrationRecycler.adapter = adapter
            }
            adapter?.updateDataSet(state.mangaForSource)
        }
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter?.getItem(position) ?: return false

        if (item is MangaItem) {
            val controller = SearchController(item.manga)
            controller.targetController = this

            router.pushController(controller.withFadeTransaction())
        } else if (item is SourceItem) {
            presenter.setSelectedSource(item.source)
        }
        return false
    }

    override fun onSelectClick(position: Int) {
        onItemClick(view!!, position)
    }
}
