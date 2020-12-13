package eu.kanade.tachiyomi.ui.library

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.databinding.SourceComfortableGridItemBinding
import eu.kanade.tachiyomi.util.isLocal

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
) : LibraryHolder<SourceComfortableGridItemBinding>(view, adapter) {

    override val binding = SourceComfortableGridItemBinding.bind(view)

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param item the manga item to bind.
     */
    override fun onSetValues(item: LibraryItem) {
        // Update the title of the manga.
        binding.title.text = item.manga.title

        // For rounded corners
        binding.badges.clipToOutline = true

        // Update the unread count and its visibility.
        with(binding.unreadText) {
            isVisible = item.unreadCount > 0
            text = item.unreadCount.toString()
        }
        // Update the download count and its visibility.
        with(binding.downloadText) {
            isVisible = item.downloadCount > 0
            text = item.downloadCount.toString()
        }
        // set local visibility if its local manga
        binding.localText.isVisible = item.manga.isLocal()

        // For rounded corners
        binding.card.clipToOutline = true

        // Update the cover.
        GlideApp.with(view.context).clear(binding.thumbnail)
        GlideApp.with(view.context)
            .load(item.manga.toMangaThumbnail())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop()
            .dontAnimate()
            .into(binding.thumbnail)
    }
}
