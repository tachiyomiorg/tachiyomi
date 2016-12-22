package eu.kanade.tachiyomi.ui.recent_updates

import android.view.View
import android.widget.PopupMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.util.getResourceColor
import kotlinx.android.synthetic.main.item_recent_chapters.view.*

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
class RecentChaptersHolder(
        private val view: View,
        private val adapter: RecentChaptersAdapter,
        listener: OnListItemClickListener)
: FlexibleViewHolder(view, adapter, listener) {
    /**
     * Color of read chapter
     */
    private var readColor = view.context.getResourceColor(android.R.attr.textColorHint)

    /**
     * Color of unread chapter
     */
    private var unreadColor = view.context.getResourceColor(android.R.attr.textColorPrimary)

    /**
     * Object containing chapter information
     */
    private var chapter: RecentChapter? = null

    init {
        // We need to post a Runnable to show the popup to make sure that the PopupMenu is
        // correctly positioned. The reason being that the view may change position before the
        // PopupMenu is shown.
        view.chapter_menu.setOnClickListener { it.post({ showPopupMenu(it) }) }
    }

    /**
     * Set values of view
     *
     * @param chapter item containing chapter information
     */
    fun onSetValues(chapter: RecentChapter) {
        this.chapter = chapter

        // Set chapter title
        view.chapter_title.text = chapter.name

        // Set manga title
        view.manga_title.text = chapter.manga.title

        // Check if chapter is read and set correct color
        if (chapter.read) {
            view.chapter_title.setTextColor(readColor)
            view.manga_title.setTextColor(readColor)
        } else {
            view.chapter_title.setTextColor(unreadColor)
            view.manga_title.setTextColor(unreadColor)
        }

        // Set chapter status
        notifyStatus(chapter.status)
    }

    /**
     * Updates chapter status in view.
     *
     * @param status download status
     */
    fun notifyStatus(status: Int) = with(view.download_text) {
        when (status) {
            Download.QUEUE -> setText(R.string.chapter_queued)
            Download.DOWNLOADING -> setText(R.string.chapter_downloading)
            Download.DOWNLOADED -> setText(R.string.chapter_downloaded)
            Download.ERROR -> setText(R.string.chapter_error)
            else -> text = ""
        }
    }

    /**
     * Show pop up menu
     *
     * @param view view containing popup menu.
     */
    private fun showPopupMenu(view: View) = chapter?.let { chapter ->
        // Create a PopupMenu, giving it the clicked view for an anchor
        val popup = PopupMenu(view.context, view)

        // Inflate our menu resource into the PopupMenu's Menu
        popup.menuInflater.inflate(R.menu.chapter_recent, popup.menu)

        // Hide download and show delete if the chapter is downloaded and
        if (chapter.isDownloaded) {
            popup.menu.findItem(R.id.action_download).isVisible = false
            popup.menu.findItem(R.id.action_delete).isVisible = true
        }

        // Hide mark as unread when the chapter is unread
        if (!chapter.read /*&& mangaChapter.chapter.last_page_read == 0*/) {
            popup.menu.findItem(R.id.action_mark_as_unread).isVisible = false
        }

        // Hide mark as read when the chapter is read
        if (chapter.read) {
            popup.menu.findItem(R.id.action_mark_as_read).isVisible = false
        }

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            with(adapter.fragment) {
                when (menuItem.itemId) {
                    R.id.action_download -> downloadChapter(chapter)
                    R.id.action_delete -> deleteChapter(chapter)
                    R.id.action_mark_as_read -> markAsRead(listOf(chapter))
                    R.id.action_mark_as_unread -> markAsUnread(listOf(chapter))
                }
            }

            true
        }

        // Finally show the PopupMenu
        popup.show()
    }
}