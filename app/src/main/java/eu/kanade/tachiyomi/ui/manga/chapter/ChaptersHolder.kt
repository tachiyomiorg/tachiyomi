package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.PopupMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import kotlinx.android.synthetic.main.item_chapter.view.*
import rx.Observable
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class ChaptersHolder(private val view: View, private val adapter: ChaptersAdapter, listener: FlexibleViewHolder.OnListItemClickListener) :
        FlexibleViewHolder(view, adapter, listener) {

    private val readColor = ContextCompat.getColor(view.context, R.color.hint_text)
    private val unreadColor = ContextCompat.getColor(view.context, R.color.primary_text)
    private val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })
    private val df = DateFormat.getDateInstance(DateFormat.SHORT)

    private var item: Chapter? = null

    init {
        view.chapter_menu.setOnClickListener { v -> v.post { showPopupMenu(v) } }
    }

    fun onSetValues(chapter: Chapter, manga: Manga?) = with(view) {
        item = chapter

        val name: String
        when (manga?.displayMode) {
            Manga.DISPLAY_NUMBER -> {
                val formattedNumber = decimalFormat.format(chapter.chapter_number.toDouble())
                name = context.getString(R.string.display_mode_chapter, formattedNumber)
            }
            else -> name = chapter.name
        }

        chapter_title.text = name
        chapter_title.setTextColor(if (chapter.read) readColor else unreadColor)

        chapter_date.text = df.format(Date(chapter.date_upload))
        chapter_date.setTextColor(if (chapter.read) readColor else unreadColor)

        if (!chapter.read && chapter.last_page_read > 0) {
            chapter_pages.text = context.getString(R.string.chapter_progress, chapter.last_page_read + 1)
        } else {
            chapter_pages.text = ""
        }

        notifyStatus(chapter.status)
    }

    fun notifyStatus(status: Int) = with(view) {
        when (status) {
            Download.QUEUE -> download_text.setText(R.string.chapter_queued)
            Download.DOWNLOADING -> download_text.setText(R.string.chapter_downloading)
            Download.DOWNLOADED -> download_text.setText(R.string.chapter_downloaded)
            Download.ERROR -> download_text.setText(R.string.chapter_error)
            else -> download_text.text = ""
        }
    }

    fun onProgressChange(context: Context, downloaded: Int, total: Int) {
        view.download_text.text = context.getString(
                R.string.chapter_downloading_progress, downloaded, total)
    }

    private fun showPopupMenu(view: View) = item?.let { item ->
        // Create a PopupMenu, giving it the clicked view for an anchor
        val popup = PopupMenu(view.context, view)

        // Inflate our menu resource into the PopupMenu's Menu
        popup.menuInflater.inflate(R.menu.chapter_single, popup.menu)

        // Hide download and show delete if the chapter is downloaded
        if (item.isDownloaded) {
            popup.menu.findItem(R.id.action_download).isVisible = false
            popup.menu.findItem(R.id.action_delete).isVisible = true
        }

        // Hide mark as unread when the chapter is unread
        if (!item.read && item.last_page_read == 0) {
            popup.menu.findItem(R.id.action_mark_as_unread).isVisible = false
        }

        // Hide mark as read when the chapter is read
        if (item.read) {
            popup.menu.findItem(R.id.action_mark_as_read).isVisible = false
        }

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            val chapter = Observable.just(item)

            when (menuItem.itemId) {
                R.id.action_download -> adapter.fragment.onDownload(chapter)
                R.id.action_delete -> adapter.fragment.onDelete(chapter)
                R.id.action_mark_as_read -> adapter.fragment.onMarkAsRead(chapter)
                R.id.action_mark_as_unread -> adapter.fragment.onMarkAsUnread(chapter)
                R.id.action_mark_previous_as_read -> adapter.fragment.onMarkPreviousAsRead(item)
            }
            true
        }

        // Finally show the PopupMenu
        popup.show()
    }

}
