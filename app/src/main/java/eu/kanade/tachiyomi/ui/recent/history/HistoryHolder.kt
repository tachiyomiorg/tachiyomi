package eu.kanade.tachiyomi.ui.recent.history

import android.view.View
import coil.clear
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.databinding.HistoryItemBinding
import eu.kanade.tachiyomi.util.lang.toTimestampString
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
class HistoryHolder(
    view: View,
    val adapter: HistoryAdapter
) : FlexibleViewHolder(view, adapter) {

    private val binding = HistoryItemBinding.bind(view)

    init {
        binding.holder.setOnClickListener {
            adapter.itemClickListener.onItemClick(bindingAdapterPosition)
        }

        binding.remove.setOnClickListener {
            adapter.removeClickListener.onRemoveClick(bindingAdapterPosition)
        }

        binding.resume.setOnClickListener {
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
        binding.mangaTitle.text = manga.title

        // Set chapter number + timestamp
        if (chapter.chapter_number > -1f) {
            val formattedNumber = adapter.decimalFormat.format(chapter.chapter_number.toDouble())
            binding.mangaSubtitle.text = itemView.context.getString(
                R.string.recent_manga_time,
                formattedNumber,
                Date(history.last_read).toTimestampString()
            )
        } else {
            binding.mangaSubtitle.text = Date(history.last_read).toTimestampString()
        }

        // Set cover
        val radius = itemView.context.resources.getDimension(R.dimen.card_radius)
        binding.cover.clear()
        binding.cover.loadAny(item.manga) {
            transformations(RoundedCornersTransformation(radius))
        }
    }
}
