package eu.kanade.tachiyomi.ui.catalogue.global_search.card

import android.annotation.SuppressLint
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.widget.StateImageViewTarget
import kotlinx.android.synthetic.main.catalogue_global_search_controller_card_item.view.*

@SuppressLint("ViewConstructor")
class CatalogueSearchCardHolder(view: View, val adapter: CatalogueSearchCardAdapter) : FlexibleViewHolder(view, adapter) {

    init {
        // Call onMangaClickListener when item is pressed.
        itemView.setOnClickListener {
            val item = adapter.getItem(adapterPosition)
            adapter.mangaClickListener.OnMangaClickListener(item.manga)
        }
    }

    fun bind(manga: Manga) {
        itemView.tvTitle.text = manga.title

        // Update circle letter image.
        setImage(manga)
    }

    fun setImage(manga: Manga) {
        itemView.post {
            Glide.clear(itemView.itemImage)
            if (!manga.thumbnail_url.isNullOrEmpty()) {
                Glide.with(itemView.context)
                        .load(manga)
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .centerCrop()
                        .skipMemoryCache(true)
                        .placeholder(android.R.color.transparent)
                        .into(StateImageViewTarget(itemView.itemImage, itemView.progress))

            }
        }
    }
}