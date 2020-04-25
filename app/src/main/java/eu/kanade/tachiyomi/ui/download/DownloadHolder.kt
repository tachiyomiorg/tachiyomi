package eu.kanade.tachiyomi.ui.download

import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.view.popupMenu
import kotlinx.android.synthetic.main.download_item.chapter_title
import kotlinx.android.synthetic.main.download_item.download_progress
import kotlinx.android.synthetic.main.download_item.download_progress_text
import kotlinx.android.synthetic.main.download_item.manga_full_title
import kotlinx.android.synthetic.main.download_item.menu
import kotlinx.android.synthetic.main.download_item.reorder

/**
 * Class used to hold the data of a download.
 * All the elements from the layout file "download_item" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @constructor creates a new download holder.
 */
class DownloadHolder(private val view: View, val adapter: DownloadAdapter) :
    BaseFlexibleViewHolder(view, adapter) {

    init {
        setDragHandleView(reorder)
        menu.setOnClickListener { it.post { showPopupMenu(it) } }
    }

    private lateinit var download: Download

    /**
     * Binds this holder with the given category.
     *
     * @param category The category to bind.
     */
    fun bind(download: Download) {
        this.download = download
        // Update the chapter name.
        chapter_title.text = download.chapter.name

        // Update the manga title
        manga_full_title.text = download.manga.title

        // Update the progress bar and the number of downloaded pages
        val pages = download.pages
        if (pages == null) {
            download_progress.progress = 0
            download_progress.max = 1
            download_progress_text.text = ""
        } else {
            download_progress.max = pages.size * 100
            notifyProgress()
            notifyDownloadedPages()
        }
    }

    /**
     * Updates the progress bar of the download.
     */
    fun notifyProgress() {
        val pages = download.pages ?: return
        if (download_progress.max == 1) {
            download_progress.max = pages.size * 100
        }
        download_progress.progress = download.totalProgress
    }

    /**
     * Updates the text field of the number of downloaded pages.
     */
    fun notifyDownloadedPages() {
        val pages = download.pages ?: return
        download_progress_text.text = "${download.downloadedImages}/${pages.size}"
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        adapter.downloadItemListener.onItemReleased(position)
    }

    private fun showPopupMenu(view: View) {
        view.popupMenu(
            R.menu.download_single,
            {
                findItem(R.id.move_to_top).isVisible = bindingAdapterPosition != 0
                findItem(R.id.move_to_bottom).isVisible =
                    bindingAdapterPosition != adapter.itemCount - 1
            },
            {
                adapter.downloadItemListener.onMenuItemClick(bindingAdapterPosition, this)
                true
            }
        )
    }
}
