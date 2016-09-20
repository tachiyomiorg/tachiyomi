package eu.kanade.tachiyomi.ui.latest_updates

import android.view.View
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.getResourceColor
import kotlinx.android.synthetic.main.item_catalogue_list.view.*

/**
 * Class used to hold the displayed data of a manga in the catalogue, like the cover or the title.
 */
class LatestUpdatesListHolder(private val view: View, adapter: LatestUpdatesAdapter, listener: OnListItemClickListener) :
        LatestUpdatesHolder(view, adapter, listener) {

    private val favoriteColor = view.context.theme.getResourceColor(android.R.attr.textColorHint)
    private val unfavoriteColor = view.context.theme.getResourceColor(android.R.attr.textColorPrimary)

    /**
     * Updates the data for this holder with the given manga.
     */
    override fun onSetValues(manga: Manga) {
        view.title.text = manga.title
        view.title.setTextColor(if (manga.favorite) favoriteColor else unfavoriteColor)
    }
}
