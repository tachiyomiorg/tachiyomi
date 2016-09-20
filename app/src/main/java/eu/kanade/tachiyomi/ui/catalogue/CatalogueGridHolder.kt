package eu.kanade.tachiyomi.ui.catalogue

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.data.database.models.Manga
import kotlinx.android.synthetic.main.item_catalogue_grid.view.*

/**
 * Class used to hold the displayed data of a manga in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_catalogue_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new catalogue holder.
 */
class CatalogueGridHolder(private val view: View, private val adapter: CatalogueAdapter, listener: OnListItemClickListener) :
        CatalogueHolder(view, adapter, listener) {

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    override fun onSetValues(manga: Manga) {
        // Set manga title
        view.title.text = manga.title

        // Set alpha of thumbnail.
        view.thumbnail.alpha = if (manga.favorite) 0.3f else 1.0f

        setImage(manga)
    }

    /**
     * Updates the image for this holder. Useful to update the image when the manga is initialized
     * and the url is now known.
     *
     * @param manga the manga to bind.
     */
    fun setImage(manga: Manga) {
        Glide.clear(view.thumbnail)
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            Glide.with(view.context)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .centerCrop()
                    .skipMemoryCache(true)
                    .placeholder(android.R.color.transparent)
                    .into(view.thumbnail)

        }
    }
}