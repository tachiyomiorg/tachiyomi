package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.GlobalSearchControllerCardBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.util.system.LocaleHelper

/**
 * Holder that binds the [GlobalSearchItem] containing catalogue cards.
 *
 * @param view view of [GlobalSearchItem]
 * @param adapter instance of [GlobalSearchAdapter]
 */
class GlobalSearchHolder(view: View, val adapter: GlobalSearchAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = GlobalSearchControllerCardBinding.bind(view)

    /**
     * Adapter containing manga from search results.
     */
    private val mangaAdapter = GlobalSearchCardAdapter(adapter.controller)

    private var lastBoundResults: List<GlobalSearchCardItem>? = null

    init {
        // Set layout horizontal.
        binding.recycler.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        binding.recycler.adapter = mangaAdapter

        binding.titleWrapper.setOnClickListener {
            adapter.getItem(bindingAdapterPosition)?.let {
                adapter.titleClickListener.onTitleClick(it.source)
            }
        }
    }

    /**
     * Show the loading of source search result.
     *
     * @param item item of card.
     */
    fun bind(item: GlobalSearchItem) {
        val source = item.source
        val results = item.results

        val titlePrefix = if (item.highlighted) "▶ " else ""

        binding.title.text = titlePrefix + source.name
        binding.subtitle.isVisible = source !is LocalSource
        binding.subtitle.text = LocaleHelper.getDisplayName(source.lang)

        when {
            results == null -> {
                binding.progress.isVisible = true
                showResultsHolder()
            }
            results.isEmpty() -> {
                binding.progress.isVisible = false
                showNoResults()
            }
            else -> {
                binding.progress.isVisible = false
                showResultsHolder()
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

    private fun showResultsHolder() {
        binding.noResultsFound.isVisible = false
    }

    private fun showNoResults() {
        binding.noResultsFound.isVisible = true
    }
}
