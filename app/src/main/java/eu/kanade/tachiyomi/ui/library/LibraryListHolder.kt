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
import eu.kanade.tachiyomi.databinding.SourceListItemBinding
import eu.kanade.tachiyomi.util.isLocal

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
) : LibraryHolder<SourceListItemBinding>(view, adapter) {

    override val binding = SourceListItemBinding.bind(view)

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
            text = "${item.downloadCount}"
        }
        // show local text badge if local manga
        binding.localText.isVisible = item.manga.isLocal()

        // Create thumbnail onclick to simulate long click
        binding.thumbnail.setOnClickListener {
            // Simulate long click on this view to enter selection mode
            onLongClick(itemView)
        }

        // Update the cover.
        GlideApp.with(itemView.context).clear(binding.thumbnail)

        val radius = view.context.resources.getDimensionPixelSize(R.dimen.card_radius)
        val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(radius))
        GlideApp.with(itemView.context)
            .load(item.manga.toMangaThumbnail())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .apply(requestOptions)
            .dontAnimate()
            .into(binding.thumbnail)
    }
}
