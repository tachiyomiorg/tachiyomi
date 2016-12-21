package eu.kanade.tachiyomi.ui.download

import android.support.v7.widget.RecyclerView
import android.view.View
import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.android.synthetic.main.item_download.view.*

/**
 * Class used to hold the data of a download.
 * All the elements from the layout file "item_download" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @constructor creates a new download holder.
 */
class DownloadHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private lateinit var download: Download

    /**
     * Method called from [DownloadAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given download.
     *
     * @param download the download to bind.
     */
    fun onSetValues(download: Download) {
        this.download = download

        // Update the chapter name.
        view.chapter_title.text = download.chapter.name

        // Update the manga title
        view.manga_title.text = download.manga.title

        // Update the progress bar and the number of downloaded pages
        val pages = download.pages
        if (pages == null) {
            view.download_progress.progress = 0
            view.download_progress.max = 1
            view.download_progress_text.text = ""
        } else {
            view.download_progress.max = pages.size * 100
            notifyProgress()
            notifyDownloadedPages()
        }
    }

    /**
     * Updates the progress bar of the download.
     */
    fun notifyProgress() {
        val pages = download.pages ?: return
        if (view.download_progress.max == 1) {
            view.download_progress.max = pages.size * 100
        }
        view.download_progress.progress = download.totalProgress
    }

    /**
     * Updates the text field of the number of downloaded pages.
     */
    fun notifyDownloadedPages() {
        val pages = download.pages ?: return
        view.download_progress_text.text = "${download.downloadedImages}/${pages.size}"
    }

}
