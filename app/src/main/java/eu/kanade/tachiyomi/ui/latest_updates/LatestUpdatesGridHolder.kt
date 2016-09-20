package eu.kanade.tachiyomi.ui.latest_updates

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.data.database.models.Manga
import kotlinx.android.synthetic.main.item_catalogue_grid.view.*

/**
 * Class used to hold the displayed data of a manga in the catalogue, like the cover or the title.
 */
class LatestUpdatesGridHolder(private val view: View, private val adapter: LatestUpdatesAdapter, listener: OnListItemClickListener) :
        LatestUpdatesHolder(view, adapter, listener) {

    /**
     * Updates the data for this holder with the given manga.
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