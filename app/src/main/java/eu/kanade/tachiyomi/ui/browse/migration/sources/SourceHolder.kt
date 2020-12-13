package eu.kanade.tachiyomi.ui.browse.migration.sources

import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.databinding.SourceMainControllerCardItemBinding
import eu.kanade.tachiyomi.source.icon

class SourceHolder(view: View, val adapter: SourceAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = SourceMainControllerCardItemBinding.bind(view)

    fun bind(item: SourceItem) {
        val source = item.source

        // Set source name
        binding.title.text = source.name

        // Set source icon
        itemView.post {
            binding.image.setImageDrawable(source.icon())
        }
    }
}
