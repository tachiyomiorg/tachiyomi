package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.support.v7.widget.LinearLayoutManager
import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.catalogue.global_search.card.CatalogueSearchCardAdapter
import eu.kanade.tachiyomi.ui.catalogue.global_search.card.CatalogueSearchCardHolder
import eu.kanade.tachiyomi.ui.catalogue.global_search.card.CatalogueSearchCardItem
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


    fun bind(searchResult: Pair<List<Manga>, CatalogueSource>) {
        val source = searchResult.second
        val mangas = searchResult.first

        with(itemView) {
            // Set Title witch country code if available.
            title.text = if (!source.lang.isEmpty()) "${source.name} (${source.lang})" else source.name

            if (!mangas.isEmpty()) {
                // Show search results.
                itemView.nothing_found.visibility = View.GONE
                itemView.recycler.visibility = View.VISIBLE

                // Set layout horizontal.
                recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                recycler.isNestedScrollingEnabled = false

                // Set adapter.
                mangaAdapter = CatalogueSearchCardAdapter(adapter.controller)
                recycler.adapter = mangaAdapter

                // Update data set.
                mangaAdapter?.updateDataSet(searchResult.first.map(::CatalogueSearchCardItem))
            } else {
                itemView.nothing_found.visibility = View.VISIBLE
                itemView.recycler.visibility = View.GONE
            }
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
        val adapter = mangaAdapter ?: return null

        adapter.allBoundViewHolders.forEach { holder ->
            val item = adapter.getItem(holder.adapterPosition)
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as CatalogueSearchCardHolder
            }
        }

        return null
    }
}
