package eu.kanade.tachiyomi.ui.library

import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.util.isLocal
import kotlinx.android.synthetic.main.source_list_item.badges
import kotlinx.android.synthetic.main.source_list_item.download_text
import kotlinx.android.synthetic.main.source_list_item.local_text
import kotlinx.android.synthetic.main.source_list_item.thumbnail
import kotlinx.android.synthetic.main.source_list_item.title
import kotlinx.android.synthetic.main.source_list_item.unread_text

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
            text = "${item.downloadCount}"
        }
        // show local text badge if local manga
        local_text.isVisible = item.manga.isLocal()

        // Create thumbnail onclick to simulate long click
        thumbnail.setOnClickListener {
            // Simulate long click on this view to enter selection mode
            onLongClick(itemView)
        }

        // Update the cover.
        GlideApp.with(itemView.context).clear(thumbnail)

        val radius = view.context.resources.getDimensionPixelSize(R.dimen.card_radius)
        val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(radius))
        GlideApp.with(itemView.context)
            .load(item.manga.toMangaThumbnail())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .apply(requestOptions)
            .dontAnimate()
            .into(thumbnail)
    }
}
