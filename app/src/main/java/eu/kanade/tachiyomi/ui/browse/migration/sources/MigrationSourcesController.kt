package eu.kanade.tachiyomi.ui.browse.migration.sources

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.databinding.MigrationSourcesControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrationMangaController

class MigrationSourcesController :
    NucleusController<MigrationSourcesControllerBinding, MigrationSourcesPresenter>(),
    FlexibleAdapter.OnItemClickListener {

    private var adapter: SourceAdapter? = null

    override fun createPresenter(): MigrationSourcesPresenter {
        return MigrationSourcesPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = MigrationSourcesControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = SourceAdapter(this)
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter
        adapter?.fastScroller = binding.fastScroller
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    fun setSources(sourcesWithManga: List<SourceItem>) {
        adapter?.updateDataSet(sourcesWithManga)
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter?.getItem(position) as? SourceItem ?: return false
        val controller = MigrationMangaController(item.source.id, item.source.name)
        parentController!!.router.pushController(controller.withFadeTransaction())
        return false
    }
}
