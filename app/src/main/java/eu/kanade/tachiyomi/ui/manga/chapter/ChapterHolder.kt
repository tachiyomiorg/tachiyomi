package eu.kanade.tachiyomi.ui.manga.chapter

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import java.util.Date
import kotlinx.android.synthetic.main.chapters_item.bookmark_icon
import kotlinx.android.synthetic.main.chapters_item.chapter_description
import kotlinx.android.synthetic.main.chapters_item.chapter_title
import kotlinx.android.synthetic.main.chapters_item.download_text

class ChapterHolder(
    view: View,
    private val adapter: ChaptersAdapter
) : BaseFlexibleViewHolder(view, adapter) {

    fun bind(item: ChapterItem, manga: Manga) {
        val chapter = item.chapter

        chapter_title.text = when (manga.displayMode) {
            Manga.DISPLAY_NUMBER -> {
                val number = adapter.decimalFormat.format(chapter.chapter_number.toDouble())
                itemView.context.getString(R.string.display_mode_chapter, number)
            }
            else -> chapter.name
        }

        // Set correct text color
        val chapterColor = when {
            chapter.read -> adapter.readColor
            chapter.bookmark -> adapter.bookmarkedColor
            else -> adapter.unreadColor
        }
        chapter_title.setTextColor(chapterColor)
        chapter_description.setTextColor(chapterColor)

        bookmark_icon.isVisible = chapter.bookmark

        val descriptions = mutableListOf<CharSequence>()

        if (chapter.date_upload > 0) {
            descriptions.add(adapter.dateFormat.format(Date(chapter.date_upload)))
        }
        if (!chapter.read && chapter.last_page_read > 0) {
            val lastPageRead = SpannableString(itemView.context.getString(R.string.chapter_progress, chapter.last_page_read + 1)).apply {
                setSpan(ForegroundColorSpan(adapter.readColor), 0, length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            descriptions.add(lastPageRead)
        }
        if (!chapter.scanlator.isNullOrBlank()) {
            descriptions.add(chapter.scanlator!!)
        }

        if (descriptions.isNotEmpty()) {
            chapter_description.text = descriptions.joinTo(SpannableStringBuilder(), " • ")
        } else {
            chapter_description.text = ""
        }

        notifyStatus(item.status)
    }

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
