package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.widget.StateImageViewTarget
import kotlinx.android.synthetic.main.catalogue_global_search_controller_card_item.*

class CatalogueSearchCardHolder(view: View, adapter: CatalogueSearchCardAdapter)
    : BaseFlexibleViewHolder(view, adapter) {

    init {
        // Call onMangaClickListener when item is pressed.
        itemView.setOnClickListener {
            val item = adapter.getItem(adapterPosition)
            if (item != null) {
                adapter.mangaClickListener.onMangaClick(item.manga)
            }
        }
        itemView.setOnLongClickListener {
            val item = adapter.getItem(adapterPosition)
            if (item != null) {
                adapter.mangaClickListener.onMangaLongClick(item.manga)
            }
            true
        }
    }

    fun bind(manga: Manga) {
        tvTitle.text = manga.title
        // Set alpha of thumbnail.
        itemImage.alpha = if (manga.favorite) 0.3f else 1.0f

        setImage(manga)
    }

    fun setImage(manga: Manga) {
        GlideApp.with(itemView.context).clear(itemImage)
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            GlideApp.with(itemView.context)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .centerCrop()
                    .skipMemoryCache(true)
                    .placeholder(android.R.color.transparent)
                    .into(StateImageViewTarget(itemImage, progress))
        }
    }

}