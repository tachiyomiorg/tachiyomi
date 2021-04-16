package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import android.view.View
import coil.clear
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import eu.davidea.viewholders.FlexibleViewHolder
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

        binding.title.text = manga.title
        // Set alpha of thumbnail.
        binding.cover.alpha = if (manga.favorite) 0.3f else 1.0f

        setImage(manga)
    }

    fun setImage(manga: Manga) {
        binding.cover.clear()
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            val request = ImageRequest.Builder(itemView.context)
                .data(manga)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .target(StateImageViewTarget(binding.cover, binding.progress))
                .build()
            itemView.context.imageLoader.enqueue(request)
        }
    }
}
