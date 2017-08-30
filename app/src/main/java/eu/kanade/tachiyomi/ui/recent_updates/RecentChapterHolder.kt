package eu.kanade.tachiyomi.ui.recent_updates

import android.view.View
import android.widget.PopupMenu
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.setVectorCompat
import jp.wasabeef.glide.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.recent_chapters_item.view.*

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
class RecentChapterHolder(private val view: View, private val adapter: RecentChaptersAdapter) :
        FlexibleViewHolder(view, adapter) {

    /**
     * Color of read chapter
     */
    private var readColor = view.context.getResourceColor(android.R.attr.textColorHint)

    /**
     * Color of unread chapter
     */
    private var unreadColor = view.context.getResourceColor(android.R.attr.textColorPrimary)

    /**
     * Currently bound item.
     */
    private var item: RecentChapterItem? = null

    init {
        // Set the correct drawable for dropdown and update the tint to match theme.
        view.chapter_menu_icon.setVectorCompat(R.drawable.ic_more_horiz_black_24dp, view.context.getResourceColor(R.attr.icon_color))

        // We need to post a Runnable to show the popup to make sure that the PopupMenu is
        // correctly positioned. The reason being that the view may change position before the
        // PopupMenu is shown.
        view.chapter_menu.setOnClickListener { it.post { showPopupMenu(it) } }
        view.manga_cover.setOnClickListener {
            adapter.coverClickListener.onCoverClick(adapterPosition)
        }
    }

    /**
     * Set values of view
     *
     * @param item item containing chapter information
     */
    fun bind(item: RecentChapterItem) {
        this.item = item

        // Set chapter title
        view.chapter_title.text = item.chapter.name

        // Set manga title
        view.manga_title.text = item.manga.title

        // Set cover
        Glide.clear(itemView.manga_cover)
        if (!item.manga.thumbnail_url.isNullOrEmpty()) {
            Glide.with(itemView.context)
                    .load(item.manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .bitmapTransform(CropCircleTransformation(view.context))
                    .into(itemView.manga_cover)
        }

        // Check if chapter is read and set correct color
        if (item.chapter.read) {
            view.chapter_title.setTextColor(readColor)
            view.manga_title.setTextColor(readColor)
        } else {
            view.chapter_title.setTextColor(unreadColor)
            view.manga_title.setTextColor(unreadColor)
        }

        // Set chapter status
        notifyStatus(item.status)
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
    private fun showPopupMenu(view: View) = item?.let { item ->
        // Create a PopupMenu, giving it the clicked view for an anchor
        val popup = PopupMenu(view.context, view)

        // Inflate our menu resource into the PopupMenu's Menu
        popup.menuInflater.inflate(R.menu.chapter_recent, popup.menu)

        // Hide download and show delete if the chapter is downloaded and
        if (item.isDownloaded) {
            popup.menu.findItem(R.id.action_download).isVisible = false
            popup.menu.findItem(R.id.action_delete).isVisible = true
        }

        // Hide mark as unread when the chapter is unread
        if (!item.chapter.read /*&& mangaChapter.chapter.last_page_read == 0*/) {
            popup.menu.findItem(R.id.action_mark_as_unread).isVisible = false
        }

        // Hide mark as read when the chapter is read
        if (item.chapter.read) {
            popup.menu.findItem(R.id.action_mark_as_read).isVisible = false
        }

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            with(adapter.controller) {
                when (menuItem.itemId) {
                    R.id.action_download -> downloadChapter(item)
                    R.id.action_delete -> deleteChapter(item)
                    R.id.action_mark_as_read -> markAsRead(listOf(item))
                    R.id.action_mark_as_unread -> markAsUnread(listOf(item))
                }
            }

            true
        }

        // Finally show the PopupMenu
        popup.show()
    }
}