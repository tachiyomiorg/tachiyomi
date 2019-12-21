package eu.kanade.tachiyomi.ui.recently_read

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.recently_read_item.*
import java.util.Date

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
) : BaseFlexibleViewHolder(view, adapter) {

    init {
        remove.setOnClickListener {
            adapter.removeClickListener.onRemoveClick(adapterPosition)
        }

        resume.setOnClickListener {
            adapter.resumeClickListener.onResumeClick(adapterPosition)
        }

        cover.setOnClickListener {
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
        manga_title.text = manga.title

        // Set source + chapter title
        val formattedNumber = adapter.decimalFormat.format(chapter.chapter_number.toDouble())
        manga_source.text = itemView.context.getString(R.string.recent_manga_source)
                .format(adapter.sourceManager.getOrStub(manga.source).toString(), formattedNumber)

        // Set last read timestamp title
        last_read.text = adapter.dateFormat.format(Date(history.last_read))

        // Set cover
        GlideApp.with(itemView.context).clear(cover)
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            GlideApp.with(itemView.context)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
                    .into(cover)
        }
    }


}
