package eu.kanade.tachiyomi.ui.recent.updates

import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.getResourceColor
import kotlinx.android.synthetic.main.updates_item.chapter_title
import kotlinx.android.synthetic.main.updates_item.download_text
import kotlinx.android.synthetic.main.updates_item.manga_cover
import kotlinx.android.synthetic.main.updates_item.manga_title

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
    BaseFlexibleViewHolder(view, adapter) {

    private var readColor = ContextCompat.getColor(view.context, R.color.material_on_surface_disabled)
    private var unreadColor = view.context.getResourceColor(R.attr.colorOnSurface)

    /**
     * Currently bound item.
     */
    private var item: UpdatesItem? = null

    init {
        manga_cover.setOnClickListener {
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
        chapter_title.text = item.chapter.name

        // Set manga title
        manga_title.text = item.manga.title

        // Set cover
        GlideApp.with(itemView.context).clear(manga_cover)
        if (!item.manga.thumbnail_url.isNullOrEmpty()) {
            GlideApp.with(itemView.context)
                .load(item.manga.toMangaThumbnail())
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .circleCrop()
                .into(manga_cover)
        }

        // Check if chapter is read and set correct color
        if (item.chapter.read) {
            chapter_title.setTextColor(readColor)
            manga_title.setTextColor(readColor)
        } else {
            chapter_title.setTextColor(unreadColor)
            manga_title.setTextColor(unreadColor)
        }

        // Set chapter status
        notifyStatus(item.status)
    }

    /**
     * Updates chapter status in view.
     *
     * @param status download status
     */
    fun notifyStatus(status: Int) = with(download_text) {
        when (status) {
            Download.QUEUE -> setText(R.string.chapter_queued)
            Download.DOWNLOADING -> setText(R.string.chapter_downloading)
            Download.DOWNLOADED -> setText(R.string.chapter_downloaded)
            Download.ERROR -> setText(R.string.chapter_error)
            else -> text = ""
        }
    }
}
