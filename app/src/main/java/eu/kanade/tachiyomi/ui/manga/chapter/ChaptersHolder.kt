package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.View
import android.widget.PopupMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.util.getResourceColor
import kotlinx.android.synthetic.main.item_chapter.view.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class ChaptersHolder(
        private val view: View,
        private val adapter: ChaptersAdapter,
        listener: FlexibleViewHolder.OnListItemClickListener)
: FlexibleViewHolder(view, adapter, listener) {

    private val readColor = view.context.getResourceColor(android.R.attr.textColorHint)
    private val unreadColor = view.context.getResourceColor(android.R.attr.textColorPrimary)
    private val bookmarkedColor = view.context.getResourceColor(R.attr.colorAccent)
    private val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })
    private val df = DateFormat.getDateInstance(DateFormat.SHORT)

    private var item: ChapterModel? = null

    init {
        // We need to post a Runnable to show the popup to make sure that the PopupMenu is
        // correctly positioned. The reason being that the view may change position before the
        // PopupMenu is shown.
        view.chapter_menu.setOnClickListener { it.post { showPopupMenu(it) } }
    }

    fun onSetValues(chapter: ChapterModel, manga: Manga?) = with(view) {
        item = chapter

        chapter_title.text = when (manga?.displayMode) {
            Manga.DISPLAY_NUMBER -> {
                val formattedNumber = decimalFormat.format(chapter.chapter_number.toDouble())
                context.getString(R.string.display_mode_chapter, formattedNumber)
            }
            else -> chapter.name
        }

        // Set correct text color
        chapter_title.setTextColor(if (chapter.read) readColor else unreadColor)
        if (chapter.bookmark) chapter_title.setTextColor(bookmarkedColor)

        if (chapter.date_upload > 0) {
            chapter_date.text = df.format(Date(chapter.date_upload))
            chapter_date.setTextColor(if (chapter.read) readColor else unreadColor)
        } else {
            chapter_date.text = ""
        }

        chapter_pages.text = if (!chapter.read && chapter.last_page_read > 0) {
            context.getString(R.string.chapter_progress, chapter.last_page_read + 1)
        } else {
            ""
        }

        notifyStatus(chapter.status)
    }

    fun notifyStatus(status: Int) = with(view.download_text) {
        when (status) {
            Download.QUEUE -> setText(R.string.chapter_queued)
            Download.DOWNLOADING -> setText(R.string.chapter_downloading)
            Download.DOWNLOADED -> setText(R.string.chapter_downloaded)
            Download.ERROR -> setText(R.string.chapter_error)
            else -> text = ""
        }
    }

    private fun showPopupMenu(view: View) = item?.let { chapter ->
        // Create a PopupMenu, giving it the clicked view for an anchor
        val popup = PopupMenu(view.context, view)

        // Inflate our menu resource into the PopupMenu's Menu
        popup.menuInflater.inflate(R.menu.chapter_single, popup.menu)

        // Hide download and show delete if the chapter is downloaded
        if (chapter.isDownloaded) {
            popup.menu.findItem(R.id.action_download).isVisible = false
            popup.menu.findItem(R.id.action_delete).isVisible = true
        }

        // Hide bookmark if bookmark
        popup.menu.findItem(R.id.action_bookmark).isVisible = !chapter.bookmark
        popup.menu.findItem(R.id.action_remove_bookmark).isVisible = chapter.bookmark

        // Hide mark as unread when the chapter is unread
        if (!chapter.read && chapter.last_page_read == 0) {
            popup.menu.findItem(R.id.action_mark_as_unread).isVisible = false
        }

        // Hide mark as read when the chapter is read
        if (chapter.read) {
            popup.menu.findItem(R.id.action_mark_as_read).isVisible = false
        }

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            val chapterList = listOf(chapter)

            with(adapter.fragment) {
                when (menuItem.itemId) {
                    R.id.action_download -> downloadChapters(chapterList)
                    R.id.action_bookmark -> bookmarkChapters(chapterList, true)
                    R.id.action_remove_bookmark -> bookmarkChapters(chapterList, false)
                    R.id.action_delete -> deleteChapters(chapterList)
                    R.id.action_mark_as_read -> markAsRead(chapterList)
                    R.id.action_mark_as_unread -> markAsUnread(chapterList)
                    R.id.action_mark_previous_as_read -> markPreviousAsRead(chapter)
                }
            }

            true
        }

        // Finally show the PopupMenu
        popup.show()
    }

}
