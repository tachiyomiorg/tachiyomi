package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.support.v7.widget.LinearLayoutManager
import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.catalogue.global_search.card.CatalogueSearchCardAdapter
import eu.kanade.tachiyomi.ui.catalogue.global_search.card.CatalogueSearchCardHolder
import eu.kanade.tachiyomi.ui.catalogue.global_search.card.CatalogueSearchCardItem
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.setVectorCompat
import kotlinx.android.synthetic.main.catalogue_global_search_controller_card.view.*

/**
 * Holder that binds the [CatalogueSearchItem] containing catalogue cards.
 *
 * @param view view of [CatalogueSearchItem]
 * @param adapter instance of [CatalogueSearchAdapter]
 */
class CatalogueSearchHolder(view: View, val adapter: CatalogueSearchAdapter) : FlexibleViewHolder(view, adapter) {

    /**
     * Adapter containing manga from search results.
     */
    private var mangaAdapter: CatalogueSearchCardAdapter? = null

    init {
        with(itemView) {
            // Set layout horizontal.
            recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            recycler.isNestedScrollingEnabled = false

            // Set adapter.
            mangaAdapter = CatalogueSearchCardAdapter(adapter.controller)
            recycler.adapter = mangaAdapter
        }
    }

    /**
     * Show the loading of source search result.
     *
     * @param source source of card.
     */
    fun bind(source: CatalogueSource) {
        with(itemView) {
            // Set Title witch country code if available.
            title.text = if (!source.lang.isEmpty()) "${source.name} (${source.lang})" else source.name
            progress.visibility = View.VISIBLE
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
     * Show the results from search.
     *
     * @param result manga returned from search.
     */
    fun updateSourceFetch(result: List<Manga>) {
        itemView.post {
            itemView.progress.visibility = View.GONE
            if (!result.isEmpty()) {
                // Show search results.
                itemView.nothing_found.visibility = View.GONE
                itemView.recycler.visibility = View.VISIBLE

                // Update data set.
                mangaAdapter?.updateDataSet(result.map(::CatalogueSearchCardItem))
            } else {
                // Show no results found
                itemView.nothing_found_icon.setVectorCompat(R.drawable.ic_search_black_112dp, itemView.context.getResourceColor(android.R.attr.textColorHint))
                itemView.nothing_found.visibility = View.VISIBLE
                itemView.recycler.visibility = View.GONE
            }
        }
    }

    /**
     * Returns the view holder for the given manga.
     *
     * @param manga the manga to find.
     * @return the holder of the manga or null if it's not bound.
     */
    private fun getHolder(manga: Manga): CatalogueSearchCardHolder? {
        val adapter = mangaAdapter ?: return null

        adapter.allBoundViewHolders.forEach { holder ->
            val item = adapter.getItem(holder.adapterPosition)
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as CatalogueSearchCardHolder
            }
        }

        return null
    }

    fun clear() {
        mangaAdapter?.clear()
    }
}
