package eu.kanade.tachiyomi.ui.library

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.util.isLocal
import kotlinx.android.synthetic.main.source_comfortable_grid_item.badges
import kotlinx.android.synthetic.main.source_comfortable_grid_item.card
import kotlinx.android.synthetic.main.source_comfortable_grid_item.download_text
import kotlinx.android.synthetic.main.source_comfortable_grid_item.local_text
import kotlinx.android.synthetic.main.source_comfortable_grid_item.thumbnail
import kotlinx.android.synthetic.main.source_comfortable_grid_item.title
import kotlinx.android.synthetic.main.source_comfortable_grid_item.unread_text

/**
 * Class used to hold the displayed data of a manga in the library, like the cover or the title.
 * All the elements from the layout file "item_source_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new library holder.
 */
class LibraryComfortableGridHolder(
    private val view: View,
    adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
) : LibraryCompactGridHolder(view, adapter) {

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param item the manga item to bind.
     */
    override fun onSetValues(item: LibraryItem) {
        // Update the title of the manga.
        title.text = item.manga.title

        // For rounded corners
        badges.clipToOutline = true

        // Update the unread count and its visibility.
        with(unread_text) {
            isVisible = item.unreadCount > 0
            text = item.unreadCount.toString()
        }
        // Update the download count and its visibility.
        with(download_text) {
            isVisible = item.downloadCount > 0
            text = item.downloadCount.toString()
        }
        // set local visibility if its local manga
        local_text.isVisible = item.manga.isLocal()

        // For rounded corners
        card.clipToOutline = true

        // Update the cover.
        GlideApp.with(view.context).clear(thumbnail)
        GlideApp.with(view.context)
            .load(item.manga.toMangaThumbnail())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop()
            .dontAnimate()
            .into(thumbnail)
    }
}
