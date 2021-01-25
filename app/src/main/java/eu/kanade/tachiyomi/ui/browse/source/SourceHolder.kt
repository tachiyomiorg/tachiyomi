package eu.kanade.tachiyomi.ui.browse.source

import android.view.View
import androidx.core.view.isVisible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.SourceMainControllerCardItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.setVectorCompat

class SourceHolder(private val view: View, val adapter: SourceAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = SourceMainControllerCardItemBinding.bind(view)

    init {
        binding.sourceLatest.setOnClickListener {
            adapter.clickListener.onLatestClick(bindingAdapterPosition)
        }

        binding.pin.setOnClickListener {
            adapter.clickListener.onPinClick(bindingAdapterPosition)
        }
    }

    fun bind(item: SourceItem) {
        val source = item.source

        binding.title.text = source.name
        binding.subtitle.isVisible = source !is LocalSource
        binding.subtitle.text = LocaleHelper.getDisplayName(source.lang)

        // Set source icon
        itemView.post {
            val icon = source.icon()
            when {
                icon != null -> binding.image.setImageDrawable(icon)
                item.source.id == LocalSource.ID -> binding.image.setImageResource(R.mipmap.ic_local_source)
            }
        }

        binding.sourceLatest.isVisible = source.supportsLatest

        binding.pin.isVisible = true
        if (item.isPinned) {
            binding.pin.setVectorCompat(R.drawable.ic_push_pin_24dp, view.context.getResourceColor(R.attr.colorAccent))
        } else {
            binding.pin.setVectorCompat(R.drawable.ic_push_pin_outline_24dp, view.context.getResourceColor(android.R.attr.textColorHint))
        }
    }
}
