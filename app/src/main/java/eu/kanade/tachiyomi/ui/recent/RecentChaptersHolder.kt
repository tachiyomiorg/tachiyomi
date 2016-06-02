package eu.kanade.tachiyomi.ui.recent

import android.view.View
import android.widget.PopupMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.util.getResourceColor
import kotlinx.android.synthetic.main.item_recent_chapter.view.*

/**
 * Holder that contains chapter item
 * Uses R.layout.item_recent_chapter.
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new recent chapter holder.
 */
class RecentChaptersHolder(view: View, private val adapter: RecentChaptersAdapter, listener: FlexibleViewHolder.OnListItemClickListener) :
        FlexibleViewHolder(view, adapter, listener) {
    /**
     * Color of read chapter
     */
    private var readColor = view.context.theme.getResourceColor(android.R.attr.textColorHint)

    /**
     * Color of unread chapter
     */
    private var unreadColor = view.context.theme.getResourceColor(android.R.attr.textColorPrimary)

    /**
     * Object containing chapter information
     */
    private var mangaChapter: MangaChapter? = null

    init {
        // We need to post a Runnable to show the popup to make sure that the PopupMenu is
        // correctly positioned. The reason being that the view may change position before the
        // PopupMenu is shown.
        itemView.chapter_menu.setOnClickListener { it.post({ showPopupMenu(it) }) }
    }

    /**
     * Set values of view
     *
     * @param item item containing chapter information
     */
    fun onSetValues(item: MangaChapter) {
        this.mangaChapter = item

        // Set chapter title
        itemView.chapter_title.text = item.chapter.name

        // Set manga title
        itemView.manga_title.text = item.manga.title

        // Check if chapter is read and set correct color
        if (item.chapter.read) {
            itemView.chapter_title.setTextColor(readColor)
            itemView.manga_title.setTextColor(readColor)
        } else {
            itemView.chapter_title.setTextColor(unreadColor)
            itemView.manga_title.setTextColor(unreadColor)
        }

        // Set chapter status
        onStatusChange(item.chapter.status)
    }

    /**
     * Updates chapter status in view.

     * @param status download status
     */
    fun onStatusChange(status: Int) {
        when (status) {
            Download.QUEUE -> itemView.download_text.setText(R.string.chapter_queued)
            Download.DOWNLOADING -> itemView.download_text.setText(R.string.chapter_downloading)
            Download.DOWNLOADED -> itemView.download_text.setText(R.string.chapter_downloaded)
            Download.ERROR -> itemView.download_text.setText(R.string.chapter_error)
            else -> itemView.download_text.text = ""
        }
    }

    /**
     * Show pop up menu
     * @param view view containing popup menu.
     */
    private fun showPopupMenu(view: View) {
        // Create a PopupMenu, giving it the clicked view for an anchor
        val popup = PopupMenu(adapter.fragment.activity, view)

        // Inflate our menu resource into the PopupMenu's Menu
        popup.menuInflater.inflate(R.menu.chapter_recent, popup.menu)

        mangaChapter?.let {

            // Hide download and show delete if the chapter is downloaded and
            if (it.chapter.isDownloaded) {
                val menu = popup.menu
                menu.findItem(R.id.action_download).isVisible = false
                menu.findItem(R.id.action_delete).isVisible = true
            }

            // Hide mark as unread when the chapter is unread
            if (!it.chapter.read /*&& mangaChapter.chapter.last_page_read == 0*/) {
                popup.menu.findItem(R.id.action_mark_as_unread).isVisible = false
            }

            // Hide mark as read when the chapter is read
            if (it.chapter.read) {
                popup.menu.findItem(R.id.action_mark_as_read).isVisible = false
            }


            // Set a listener so we are notified if a menu item is clicked
            popup.setOnMenuItemClickListener { menuItem ->

                when (menuItem.itemId) {
                    R.id.action_download -> adapter.fragment.downloadChapter(it)
                    R.id.action_delete -> adapter.fragment.deleteChapter(it)
                    R.id.action_mark_as_read -> adapter.fragment.markAsRead(listOf(it))
                    R.id.action_mark_as_unread -> adapter.fragment.markAsUnread(listOf(it))
                }
                true
            }

        }

        // Finally show the PopupMenu
        popup.show()
    }
}