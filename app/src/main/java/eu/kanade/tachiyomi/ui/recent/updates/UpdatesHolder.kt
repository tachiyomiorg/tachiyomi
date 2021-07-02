package eu.kanade.tachiyomi.ui.recent.updates

import android.view.View
import androidx.core.view.isVisible
import coil.clear
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.UpdatesItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.manga.chapter.base.BaseChapterHolder

/**
 * Holder that contains chapter item
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new recent chapter holder.
 */
class UpdatesHolder(private val view: View, private val adapter: UpdatesAdapter) :
    BaseChapterHolder(view, adapter) {

    private val binding = UpdatesItemBinding.bind(view)

    init {
        binding.mangaCover.setOnClickListener {
            adapter.coverClickListener.onCoverClick(bindingAdapterPosition)
        }

        binding.download.setOnClickListener {
            onDownloadClick(it, bindingAdapterPosition)
        }
    }

    fun bind(item: UpdatesItem) {
        // Set chapter title
        binding.chapterTitle.text = item.chapter.name

        // Set manga title
        binding.mangaTitle.text = item.manga.title

        // Check if chapter is read and set correct color
        if (item.chapter.read) {
            binding.chapterTitle.setTextColor(adapter.readColor)
            binding.mangaTitle.setTextColor(adapter.readColor)
        } else {
            binding.chapterTitle.setTextColor(adapter.unreadColor)
            binding.mangaTitle.setTextColor(adapter.unreadColor)
        }

        // Set chapter status
        binding.download.isVisible = item.manga.source != LocalSource.ID
        binding.download.setState(item.status, item.progress)

        // Set cover
        val radius = itemView.context.resources.getDimension(R.dimen.card_radius)
        binding.mangaCover.clear()
        binding.mangaCover.loadAny(item.manga) {
            transformations(RoundedCornersTransformation(radius))
        }
    }
}
