package eu.kanade.tachiyomi.ui.catalogue

import android.view.View
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
class CatalogueGridHolder(private val view: View, adapter: CatalogueAdapter, listener: OnListItemClickListener) :
        CatalogueHolder(view, adapter, listener) {

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     * @param presenter the catalogue presenter.
     */
    override fun onSetValues(manga: Manga, presenter: CataloguePresenter) {
        // Set manga title
        view.title.text = manga.title

        // Set visibility of in library icon.
//        view.favorite_sticker.visibility = if (manga.favorite) View.VISIBLE else View.GONE

        // Set alpha of thumbnail.
        view.thumbnail.alpha = if (manga.favorite) 0.3f else 1.0f

        setImage(manga, presenter)
    }

    /**
     * Updates the image for this holder. Useful to update the image when the manga is initialized
     * and the url is now known.
     *
     * @param manga the manga to bind.
     * @param presenter the catalogue presenter.
     */
    fun setImage(manga: Manga, presenter: CataloguePresenter) {
        if (manga.thumbnail_url != null) {
            presenter.coverCache.loadFromNetwork(view.thumbnail, manga.thumbnail_url,
                    presenter.source.glideHeaders)
        } else {
            view.thumbnail.setImageResource(android.R.color.transparent)
        }
    }
}