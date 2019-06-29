package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.support.v7.widget.LinearLayoutManager
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.util.setVectorCompat
import eu.kanade.tachiyomi.util.visible
import kotlinx.android.synthetic.main.catalogue_global_search_controller_card.*

/**
 * Holder that binds the [CatalogueSearchItem] containing catalogue cards.
 *
 * @param view view of [CatalogueSearchItem]
 * @param adapter instance of [CatalogueSearchAdapter]
 */
class CatalogueSearchHolder(view: View, val adapter: CatalogueSearchAdapter) :
        BaseFlexibleViewHolder(view, adapter) {

    /**
     * Adapter containing manga from search results.
     */
    private val mangaAdapter = CatalogueSearchCardAdapter(adapter.controller)

    private var lastBoundResults: List<CatalogueSearchCardItem>? = null

    init {
        // Set layout horizontal.
        recycler.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        recycler.adapter = mangaAdapter
    }

    /**
     * Show the loading of source search result.
     *
     * @param item item of card.
     */
    fun bind(item: CatalogueSearchItem) {
        val source = item.source
        val results = item.results

        val titlePrefix = if (item.highlighted) "▶" else ""
        val langSuffix = if (source.lang.isNotEmpty()) " (${source.lang})" else ""

        // Set Title with country code if available.
        title.text = titlePrefix + source.name + langSuffix

        when {
            results == null -> {
                progress.visible()
                showHolder()
            }
            results.isEmpty() -> {
                progress.gone()
                hideHolder()
            }
            else -> {
                progress.gone()
                showHolder()
            }
        }
        if (results !== lastBoundResults) {
            mangaAdapter.updateDataSet(results)
            lastBoundResults = results
        }
    }

    /**
     * Called from the presenter when a manga is initialized.
     *
     * @param manga the initialized manga.
     */
    fun setImage(manga: Manga) {
        getHolder(manga)?.setImage(manga)
    }

    /**
     * Returns the view holder for the given manga.
     *
     * @param manga the manga to find.
     * @return the holder of the manga or null if it's not bound.
     */
    private fun getHolder(manga: Manga): CatalogueSearchCardHolder? {
        mangaAdapter.allBoundViewHolders.forEach { holder ->
            val item = mangaAdapter.getItem(holder.adapterPosition)
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as CatalogueSearchCardHolder
            }
        }

        return null
    }

    private fun showHolder() {
        title.visible()
        source_card.visible()
    }

    private fun hideHolder() {
        title.gone()
        source_card.gone()
    }

}
