package eu.kanade.tachiyomi.ui.recent.updates

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.databinding.UpdatesItemBinding
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * Holder that contains chapter item
 * Uses R.layout.item_recent_chapters.
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new recent chapter holder.
 */
class UpdatesHolder(private val view: View, private val adapter: UpdatesAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = UpdatesItemBinding.bind(view)

    private var readColor = view.context.getResourceColor(R.attr.colorOnSurface, 0.38f)
    private var unreadColor = view.context.getResourceColor(R.attr.colorOnSurface)

    /**
     * Currently bound item.
     */
    private var item: UpdatesItem? = null

    init {
        binding.mangaCover.setOnClickListener {
            adapter.coverClickListener.onCoverClick(bindingAdapterPosition)
        }
    }

    /**
     * Set values of view
     *
     * @param item item containing chapter information
     */
    fun bind(item: UpdatesItem) {
        this.item = item

        // Set chapter title
        binding.chapterTitle.text = item.chapter.name

        // Set manga title
        binding.mangaTitle.text = item.manga.title

        // Check if chapter is read and set correct color
        if (item.chapter.read) {
            binding.chapterTitle.setTextColor(readColor)
            binding.mangaTitle.setTextColor(readColor)
        } else {
            binding.chapterTitle.setTextColor(unreadColor)
            binding.mangaTitle.setTextColor(unreadColor)
        }

        // Set chapter status
        notifyStatus(item.status)

        // Set cover
        GlideApp.with(itemView.context).clear(binding.mangaCover)

        val radius = itemView.context.resources.getDimensionPixelSize(R.dimen.card_radius)
        val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(radius))
        GlideApp.with(itemView.context)
            .load(item.manga.toMangaThumbnail())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .apply(requestOptions)
            .dontAnimate()
            .into(binding.mangaCover)
    }

    /**
     * Updates chapter status in view.
     *
     * @param status download status
     */
    fun notifyStatus(status: Int) = with(binding.downloadText) {
        when (status) {
            Download.QUEUE -> setText(R.string.chapter_queued)
            Download.DOWNLOADING -> setText(R.string.chapter_downloading)
            Download.DOWNLOADED -> setText(R.string.chapter_downloaded)
            Download.ERROR -> setText(R.string.chapter_error)
            else -> text = ""
        }
    }
}
