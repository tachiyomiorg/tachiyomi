package eu.kanade.tachiyomi.ui.catalogue_new.search_result

import android.annotation.SuppressLint
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.widget.StateImageViewTarget
import kotlinx.android.synthetic.main.catalogue_search_single.view.*

@SuppressLint("ViewConstructor")
class CatalogueSearchSingleHolder(view: View, val adapter: CatalogueSearchSingleAdapter) : FlexibleViewHolder(view, adapter) {

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