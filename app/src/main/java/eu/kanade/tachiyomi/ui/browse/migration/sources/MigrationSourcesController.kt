package eu.kanade.tachiyomi.ui.browse.migration.sources

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MigrationSourcesControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrationMangaController
import eu.kanade.tachiyomi.util.system.openInBrowser

class MigrationSourcesController :
    NucleusController<MigrationSourcesControllerBinding, MigrationSourcesPresenter>(),
    FlexibleAdapter.OnItemClickListener {

    private var adapter: SourceAdapter? = null

    init {
        setHasOptionsMenu(true)
    }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.source_migration, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_source_migration_help -> activity?.openInBrowser(HELP_URL)
        }
        return super.onOptionsItemSelected(item)
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

    companion object {
        const val HELP_URL = "https://tachiyomi.org/help/guides/source-migration/"
    }
}
