package eu.kanade.tachiyomi.ui.manga.chapter

import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.ChaptersItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.manga.chapter.base.BaseChapterHolder
import java.util.Date

class ChapterHolder(
    view: View,
    private val adapter: ChaptersAdapter
) : BaseChapterHolder(view, adapter) {

    private val binding = ChaptersItemBinding.bind(view)

    init {
        binding.download.setOnClickListener {
            onDownloadClick(it, bindingAdapterPosition)
        }
    }

    fun bind(item: ChapterItem, manga: Manga) {
        val chapter = item.chapter

        binding.chapterTitle.text = when (manga.displayMode) {
            Manga.CHAPTER_DISPLAY_NUMBER -> {
                val number = adapter.decimalFormat.format(chapter.chapter_number.toDouble())
                itemView.context.getString(R.string.display_mode_chapter, number)
            }
            else -> chapter.name
        }

        // Set correct text color
        val chapterTitleColor = when {
            chapter.read -> adapter.readColor
            chapter.bookmark -> adapter.bookmarkedColor
            else -> adapter.unreadColor
        }
        binding.chapterTitle.setTextColor(chapterTitleColor)

        val chapterDescriptionColor = when {
            chapter.read -> adapter.readColor
            chapter.bookmark -> adapter.bookmarkedColor
            else -> adapter.unreadColorSecondary
        }
        binding.chapterDescription.setTextColor(chapterDescriptionColor)

        binding.bookmarkIcon.isVisible = chapter.bookmark

        val descriptions = mutableListOf<CharSequence>()

        if (chapter.date_upload > 0) {
            descriptions.add(adapter.dateFormat.format(Date(chapter.date_upload)))
        }
        if (!chapter.read && chapter.last_page_read > 0) {
            val lastPageRead = buildSpannedString {
                color(adapter.readColor) {
                    append(itemView.context.getString(R.string.chapter_progress, chapter.last_page_read + 1))
                }
            }
            descriptions.add(lastPageRead)
        }
        if (!chapter.scanlator.isNullOrBlank()) {
            descriptions.add(chapter.scanlator!!)
        }

        if (descriptions.isNotEmpty()) {
            binding.chapterDescription.text = descriptions.joinTo(SpannableStringBuilder(), " • ")
        } else {
            binding.chapterDescription.text = ""
        }

        binding.download.isVisible = item.manga.source != LocalSource.ID
        binding.download.setState(item.status, item.progress)
    }
}
