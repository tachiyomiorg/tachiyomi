package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.support.v7.widget.LinearLayoutManager
import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.util.setVectorCompat
import eu.kanade.tachiyomi.util.visible
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
    private val mangaAdapter = CatalogueSearchCardAdapter(adapter.controller)

    private var lastBoundResults: List<CatalogueSearchCardItem>? = null

    init {
        with(itemView) {
            // Set layout horizontal.
            recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            recycler.adapter = mangaAdapter

            nothing_found_icon.setVectorCompat(R.drawable.ic_search_black_112dp,
                    context.getResourceColor(android.R.attr.textColorHint))
        }
    }

    /**
     * Show the loading of source search result.
     *
     * @param item item of card.
     */
    fun bind(item: CatalogueSearchItem) {
        val source = item.source
        val results = item.results

        with(itemView) {
            // Set Title witch country code if available.
            title.text = if (!source.lang.isEmpty()) "${source.name} (${source.lang})" else source.name

            when {
                results == null -> {
                    progress.visible()
                    nothing_found.gone()
                }
                results.isEmpty() -> {
                    progress.gone()
                    nothing_found.visible()
                }
                else -> {
                    progress.gone()
                    nothing_found.gone()
                }
            }
            if (results !== lastBoundResults) {
                mangaAdapter.updateDataSet(results)
                lastBoundResults = results
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
        mangaAdapter.allBoundViewHolders.forEach { holder ->
            val item = mangaAdapter.getItem(holder.adapterPosition)
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as CatalogueSearchCardHolder
            }
        }

        return null
    }

}
