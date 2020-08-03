package eu.kanade.tachiyomi.ui.browse.migration.manga

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.MigrationMangaControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.SourceDividerItemDecoration
import eu.kanade.tachiyomi.ui.browse.migration.search.SearchController

class MigrationMangaController :
    NucleusController<MigrationMangaControllerBinding, MigrationMangaPresenter>,
    FlexibleAdapter.OnItemClickListener {

    private var adapter: FlexibleAdapter<IFlexible<*>>? = null

    constructor(sourceId: Long, sourceName: String?) : super(
        Bundle().apply {
            putLong(SOURCE_ID_EXTRA, sourceId)
            putString(SOURCE_NAME_EXTRA, sourceName)
        }
    )

    @Suppress("unused")
    constructor(bundle: Bundle) : this(
        bundle.getLong(SOURCE_ID_EXTRA),
        bundle.getString(SOURCE_NAME_EXTRA)
    )

    private val sourceId: Long = args.getLong(SOURCE_ID_EXTRA)
    private val sourceName: String? = args.getString(SOURCE_NAME_EXTRA)

    override fun getTitle(): String? {
        return sourceName
    }

    override fun createPresenter(): MigrationMangaPresenter {
        return MigrationMangaPresenter(sourceId)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = MigrationMangaControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = FlexibleAdapter<IFlexible<*>>(null, this)
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter
        binding.recycler.addItemDecoration(SourceDividerItemDecoration(view.context))
        adapter?.fastScroller = binding.fastScroller
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    fun setManga(manga: List<MangaItem>) {
        adapter?.updateDataSet(manga)
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter?.getItem(position) as? MangaItem ?: return false
        val controller = SearchController(item.manga)
        router.pushController(controller.withFadeTransaction())
        return false
    }

    companion object {
        const val SOURCE_ID_EXTRA = "source_id_extra"
        const val SOURCE_NAME_EXTRA = "source_name_extra"
    }
}
