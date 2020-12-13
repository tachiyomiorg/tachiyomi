package eu.kanade.tachiyomi.ui.browse.source.browse

import android.view.View
import androidx.viewbinding.ViewBinding
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.database.models.Manga

/**
 * Generic class used to hold the displayed data of a manga in the catalogue.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 */
abstract class SourceHolder<VB : ViewBinding>(view: View, adapter: FlexibleAdapter<*>) :
    FlexibleViewHolder(view, adapter) {

    abstract val binding: VB

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    abstract fun onSetValues(manga: Manga)

    /**
     * Updates the image for this holder. Useful to update the image when the manga is initialized
     * and the url is now known.
     *
     * @param manga the manga to bind.
     */
    abstract fun setImage(manga: Manga)
}
