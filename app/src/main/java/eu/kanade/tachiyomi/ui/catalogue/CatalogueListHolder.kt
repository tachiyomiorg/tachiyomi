package eu.kanade.tachiyomi.ui.catalogue

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.getResourceColor
import kotlinx.android.synthetic.main.item_catalogue_list.view.*

/**
 * Class used to hold the displayed data of a manga in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_catalogue_list" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
class CatalogueListHolder(private val view: View, adapter: FlexibleAdapter<*>) :
        CatalogueHolder(view, adapter) {

    private val favoriteColor = view.context.getResourceColor(android.R.attr.textColorHint)
    private val unfavoriteColor = view.context.getResourceColor(android.R.attr.textColorPrimary)

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    override fun onSetValues(manga: Manga) {
        view.title.text = manga.title
        view.title.setTextColor(if (manga.favorite) favoriteColor else unfavoriteColor)

        setImage(manga)
    }

    override fun setImage(manga: Manga) {
        Glide.clear(view.thumbnail)
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            Glide.with(view.context)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .centerCrop()
                    .dontAnimate()
                    .skipMemoryCache(true)
                    .placeholder(android.R.color.transparent)
                    .into(view.thumbnail)
        }
    }

}
