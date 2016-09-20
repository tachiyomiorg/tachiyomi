package eu.kanade.tachiyomi.ui.latest_updates

import android.view.View
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder

/**
 * Generic class used to hold the displayed data of a manga in the catalogue.
 */
abstract class LatestUpdatesHolder(view: View, adapter: LatestUpdatesAdapter, listener: OnListItemClickListener) :
        FlexibleViewHolder(view, adapter, listener) {

    /**
     * Updates the data for this holder with the given manga.
     */
    abstract fun onSetValues(manga: Manga)
}
