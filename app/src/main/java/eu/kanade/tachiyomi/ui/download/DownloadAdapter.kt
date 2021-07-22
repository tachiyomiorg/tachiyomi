package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem

/**
 * Adapter storing a list of downloads.
 *
 * @param context the context of the fragment containing this adapter.
 */
class DownloadAdapter(controller: DownloadController) : FlexibleAdapter<AbstractFlexibleItem<*>>(
    null,
    controller,
    true
) {

    /**
     * Listener called when an item of the list is released.
     */
    val downloadItemListener: DownloadItemListener = controller

    override fun shouldMove(fromPosition: Int, toPosition: Int): Boolean {
        // Don't let sub-items changing group
        return getHeaderOf(getItem(fromPosition)) == getHeaderOf(getItem(toPosition))
    }

    interface DownloadItemListener {
        fun onItemReleased(position: Int)
        fun onMenuItemClick(position: Int, menuItem: MenuItem)
    }
}
