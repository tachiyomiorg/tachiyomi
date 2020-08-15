package eu.kanade.tachiyomi.ui.recent.history

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.lang.toTimestampString
import java.util.Date
import kotlinx.android.synthetic.main.history_item.cover
import kotlinx.android.synthetic.main.history_item.holder
import kotlinx.android.synthetic.main.history_item.manga_subtitle
import kotlinx.android.synthetic.main.history_item.manga_title
import kotlinx.android.synthetic.main.history_item.remove
import kotlinx.android.synthetic.main.history_item.resume

/**
 * Holder that contains recent manga item
 * Uses R.layout.item_recently_read.
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new recent chapter holder.
 */
class HistoryHolder(
    view: View,
    val adapter: HistoryAdapter
) : BaseFlexibleViewHolder(view, adapter) {

    init {
        holder.setOnClickListener {
            adapter.itemClickListener.onItemClick(bindingAdapterPosition)
        }

        remove.setOnClickListener {
            adapter.removeClickListener.onRemoveClick(bindingAdapterPosition)
        }

        resume.setOnClickListener {
            adapter.resumeClickListener.onResumeClick(bindingAdapterPosition)
        }
    }

    /**
     * Set values of view
     *
     * @param item item containing history information
     */
    fun bind(item: MangaChapterHistory) {
        // Retrieve objects
        val (manga, chapter, history) = item

        // Set manga title
        manga_title.text = manga.title

        // Set chapter number + timestamp
        if (chapter.chapter_number > -1f) {
            val formattedNumber = adapter.decimalFormat.format(chapter.chapter_number.toDouble())
            manga_subtitle.text = itemView.context.getString(
                R.string.recent_manga_time,
                formattedNumber,
                Date(history.last_read).toTimestampString()
            )
        } else {
            manga_subtitle.text = Date(history.last_read).toTimestampString()
        }

        // Set cover
        GlideApp.with(itemView.context).clear(cover)
        GlideApp.with(itemView.context)
            .load(manga.toMangaThumbnail())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop()
            .into(cover)
    }
}
