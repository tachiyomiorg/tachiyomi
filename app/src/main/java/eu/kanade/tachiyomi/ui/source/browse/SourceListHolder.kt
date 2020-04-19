package eu.kanade.tachiyomi.ui.source.browse

import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.util.system.getResourceColor
import kotlinx.android.synthetic.main.source_list_item.thumbnail
import kotlinx.android.synthetic.main.source_list_item.title

/**
 * Class used to hold the displayed data of a manga in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_catalogue_list" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
class SourceListHolder(private val view: View, adapter: FlexibleAdapter<*>) :
        SourceHolder(view, adapter) {

    private val favoriteColor = ContextCompat.getColor(view.context, R.color.material_on_surface_disabled)
    private val unfavoriteColor = view.context.getResourceColor(R.attr.colorOnSurface)

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    override fun onSetValues(manga: Manga) {
        title.text = manga.title
        title.setTextColor(if (manga.favorite) favoriteColor else unfavoriteColor)

        setImage(manga)
    }

    override fun setImage(manga: Manga) {
        GlideApp.with(view.context).clear(thumbnail)
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            GlideApp.with(view.context)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .centerCrop()
                    .circleCrop()
                    .dontAnimate()
                    .placeholder(android.R.color.transparent)
                    .into(thumbnail)
        }
    }
}
