package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import android.view.View
import androidx.core.view.isVisible
import coil.clear
import coil.imageLoader
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.GlobalSearchControllerCardItemBinding
import eu.kanade.tachiyomi.widget.StateImageViewTarget

class GlobalSearchCardHolder(view: View, adapter: GlobalSearchCardAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = GlobalSearchControllerCardItemBinding.bind(view)

    init {
        // Call onMangaClickListener when item is pressed.
        itemView.setOnClickListener {
            val item = adapter.getItem(bindingAdapterPosition)
            if (item != null) {
                adapter.mangaClickListener.onMangaClick(item.manga)
            }
        }
        itemView.setOnLongClickListener {
            val item = adapter.getItem(bindingAdapterPosition)
            if (item != null) {
                adapter.mangaClickListener.onMangaLongClick(item.manga)
            }
            true
        }
    }

    fun bind(manga: Manga) {
        binding.card.clipToOutline = true

        // Set manga title
        binding.title.text = manga.title

        // Set alpha of thumbnail.
        binding.cover.alpha = if (manga.favorite) 0.3f else 1.0f

        // For rounded corners
        binding.badges.clipToOutline = true

        // Set favorite badge
        binding.favoriteText.isVisible = manga.favorite

        setImage(manga)
    }

    fun setImage(manga: Manga) {
        binding.cover.clear()
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            val crossfadeDuration = itemView.context.imageLoader.defaults.transition.let {
                if (it is CrossfadeTransition) it.durationMillis else 0
            }
            val request = ImageRequest.Builder(itemView.context)
                .data(manga)
                .setParameter(MangaCoverFetcher.USE_CUSTOM_COVER, false)
                .target(StateImageViewTarget(binding.cover, binding.progress, crossfadeDuration))
                .build()
            itemView.context.imageLoader.enqueue(request)
        }
    }
}
