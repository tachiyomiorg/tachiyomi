package eu.kanade.tachiyomi.ui.catalogue

import android.view.View
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder

/**
 * Generic class used to hold the displayed data of a manga in the catalogue.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 */
abstract class CatalogueHolder(view: View, adapter: CatalogueAdapter, listener: OnListItemClickListener) :
        FlexibleViewHolder(view, adapter, listener) {

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    abstract fun onSetValues(manga: Manga)
}
