package eu.kanade.tachiyomi.ui.browse.source.browse

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.databinding.SourceComfortableGridItemBinding
import eu.kanade.tachiyomi.widget.StateImageViewTarget

/**
 * Class used to hold the displayed data of a manga in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_source_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
class SourceComfortableGridHolder(private val view: View, private val adapter: FlexibleAdapter<*>) :
    SourceHolder<SourceComfortableGridItemBinding>(view, adapter) {

    override val binding = SourceComfortableGridItemBinding.bind(view)

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    override fun onSetValues(manga: Manga) {
        // Set manga title
        binding.title.text = manga.title

        // Set alpha of thumbnail.
        binding.thumbnail.alpha = if (manga.favorite) 0.3f else 1.0f

        setImage(manga)
    }

    override fun setImage(manga: Manga) {
        // For rounded corners
        binding.card.clipToOutline = true

        GlideApp.with(view.context).clear(binding.thumbnail)
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            GlideApp.with(view.context)
                .load(manga.toMangaThumbnail())
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .centerCrop()
                .placeholder(android.R.color.transparent)
                .into(StateImageViewTarget(binding.thumbnail, binding.progress))
        }
    }
}
