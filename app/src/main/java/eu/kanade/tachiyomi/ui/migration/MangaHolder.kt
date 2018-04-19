package eu.kanade.tachiyomi.ui.migration

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.catalogue_list_item.*

class MangaHolder(
        private val view: View,
        private val adapter: FlexibleAdapter<*>
) : BaseFlexibleViewHolder(view, adapter) {

    fun bind(item: MangaItem) {
        // Update the title of the manga.
        title.text = item.manga.title

        // Create thumbnail onclick to simulate long click
        thumbnail.setOnClickListener {
            // Simulate long click on this view to enter selection mode
            onLongClick(itemView)
        }

        // Update the cover.
        GlideApp.with(itemView.context).clear(thumbnail)
        GlideApp.with(itemView.context)
                .load(item.manga)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .circleCrop()
                .dontAnimate()
                .into(thumbnail)
    }

}
