package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.global_search_controller_card.progress
import kotlinx.android.synthetic.main.global_search_controller_card.recycler
import kotlinx.android.synthetic.main.global_search_controller_card.source_card
import kotlinx.android.synthetic.main.global_search_controller_card.title

/**
 * Holder that binds the [GlobalSearchItem] containing catalogue cards.
 *
 * @param view view of [GlobalSearchItem]
 * @param adapter instance of [GlobalSearchAdapter]
 */
class GlobalSearchHolder(view: View, val adapter: GlobalSearchAdapter) :
    BaseFlexibleViewHolder(view, adapter) {

    /**
     * Adapter containing manga from search results.
     */
    private val mangaAdapter = GlobalSearchCardAdapter(adapter.controller)

    private var lastBoundResults: List<GlobalSearchCardItem>? = null

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
    fun bind(item: GlobalSearchItem) {
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
    private fun getHolder(manga: Manga): GlobalSearchCardHolder? {
        mangaAdapter.allBoundViewHolders.forEach { holder ->
            val item = mangaAdapter.getItem(holder.bindingAdapterPosition)
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as GlobalSearchCardHolder
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
