package eu.kanade.tachiyomi.ui.library

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.source.LocalSource
import kotlinx.android.synthetic.main.catalogue_list_item.*

/**
 * Class used to hold the displayed data of a manga in the library, like the cover or the title.
 * All the elements from the layout file "item_library_list" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new library holder.
 */

class LibraryListHolder(
        private val view: View,
        private val adapter: FlexibleAdapter<*>
) : LibraryHolder(view, adapter) {

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param item the manga item to bind.
     */
    override fun onSetValues(item: LibraryItem) {
        // Update the title of the manga.
        title.text = item.manga.title

        // Update the unread count and its visibility.
        with(unread_text) {
            visibility = if (item.manga.unread > 0) View.VISIBLE else View.GONE
            text = item.manga.unread.toString()
        }
        // Update the download count and its visibility.
        with(download_text) {
            visibility = if (item.downloadCount > 0) View.VISIBLE else View.GONE
            text = "${item.downloadCount}"
        }
        //show local text badge if local manga
        local_text.visibility = if (item.manga.source == LocalSource.ID) View.VISIBLE else View.GONE

        // Create thumbnail onclick to simulate long click
        thumbnail.setOnClickListener {
            // Simulate long click on this view to enter selection mode
            onLongClick(itemView)
        }

        // Update the cover.
        GlideApp.with(itemView.context).clear(thumbnail)
        GlideApp.with(itemView.context)
                .load(item.manga)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .circleCrop()
                .dontAnimate()
                .into(thumbnail)
    }

}
