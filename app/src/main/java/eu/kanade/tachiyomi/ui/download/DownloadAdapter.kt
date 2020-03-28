package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Adapter storing a list of downloads.
 *
 * @param context the context of the fragment containing this adapter.
 */
class DownloadAdapter(controller: DownloadController) : FlexibleAdapter<DownloadItem>(
    null,
    controller,
    true
) {

    /**
     * Listener called when an item of the list is released.
     */
    val downloadItemListener: DownloadItemListener = controller

    interface DownloadItemListener {
        fun onItemReleased(position: Int)
        fun onMenuItemClick(position: Int, menuItem: MenuItem)
    }
}
