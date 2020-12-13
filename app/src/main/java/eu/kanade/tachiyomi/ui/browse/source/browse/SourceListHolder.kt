package eu.kanade.tachiyomi.ui.browse.source.browse

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.databinding.SourceListItemBinding
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * Class used to hold the displayed data of a manga in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_catalogue_list" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
class SourceListHolder(private val view: View, adapter: FlexibleAdapter<*>) :
    SourceHolder<SourceListItemBinding>(view, adapter) {

    override val binding = SourceListItemBinding.bind(view)

    private val favoriteColor = view.context.getResourceColor(R.attr.colorOnSurface, 0.38f)
    private val unfavoriteColor = view.context.getResourceColor(R.attr.colorOnSurface)

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    override fun onSetValues(manga: Manga) {
        binding.title.text = manga.title
        binding.title.setTextColor(if (manga.favorite) favoriteColor else unfavoriteColor)

        // Set alpha of thumbnail.
        binding.thumbnail.alpha = if (manga.favorite) 0.3f else 1.0f

        setImage(manga)
    }

    override fun setImage(manga: Manga) {
        GlideApp.with(view.context).clear(binding.thumbnail)

        if (!manga.thumbnail_url.isNullOrEmpty()) {
            val radius = view.context.resources.getDimensionPixelSize(R.dimen.card_radius)
            val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(radius))
            GlideApp.with(view.context)
                .load(manga.toMangaThumbnail())
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .apply(requestOptions)
                .dontAnimate()
                .placeholder(android.R.color.transparent)
                .into(binding.thumbnail)
        }
    }
}
