package eu.kanade.tachiyomi.ui.browse.migration.manga

import android.view.View
import coil.clear
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.SourceListItemBinding

class MigrationMangaHolder(
    view: View,
    private val adapter: MigrationMangaAdapter
) : FlexibleViewHolder(view, adapter) {

    private val binding = SourceListItemBinding.bind(view)

    init {
        binding.thumbnail.setOnClickListener {
            adapter.coverClickListener.onCoverClick(bindingAdapterPosition)
        }
    }

    fun bind(item: MigrationMangaItem) {
        binding.title.text = item.manga.title

        // Update the cover.
        val radius = itemView.context.resources.getDimension(R.dimen.card_radius)
        binding.thumbnail.clear()
        binding.thumbnail.loadAny(item.manga) {
            transformations(RoundedCornersTransformation(radius))
        }
    }
}
