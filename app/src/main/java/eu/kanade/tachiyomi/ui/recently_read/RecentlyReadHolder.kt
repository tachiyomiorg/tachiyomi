package eu.kanade.tachiyomi.ui.recently_read

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import kotlinx.android.synthetic.main.item_recently_read.view.*
import java.util.*

/**
 * Holder that contains recent manga item
 * Uses R.layout.item_recently_read.
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new recent chapter holder.
 */
class RecentlyReadHolder(
        view: View,
        val adapter: RecentlyReadAdapter
) : FlexibleViewHolder(view, adapter) {

    init {
        itemView.remove.setOnClickListener {
            adapter.removeClickListener.onRemoveClick(adapterPosition)
        }

        itemView.resume.setOnClickListener {
            adapter.resumeClickListener.onResumeClick(adapterPosition)
        }

        itemView.cover.setOnClickListener {
            adapter.coverClickListener.onCoverClick(adapterPosition)
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
        itemView.manga_title.text = manga.title

        // Set source + chapter title
        val formattedNumber = adapter.decimalFormat.format(chapter.chapter_number.toDouble())
        itemView.manga_source.text = itemView.context.getString(R.string.recent_manga_source)
                .format(adapter.sourceManager.get(manga.source)?.toString(), formattedNumber)

        // Set last read timestamp title
        itemView.last_read.text = adapter.dateFormat.format(Date(history.last_read))

        // Set cover
        Glide.clear(itemView.cover)
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            Glide.with(itemView.context)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .centerCrop()
                    .into(itemView.cover)
        }

    }

}
