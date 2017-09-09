package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
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
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = adapter.getItem(position)
                adapter.mangaClickListener.onMangaClick(item.manga)
            }
        }
    }

    fun bind(manga: Manga) {
        itemView.tvTitle.text = manga.title

        setImage(manga)
    }

    fun setImage(manga: Manga) {
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