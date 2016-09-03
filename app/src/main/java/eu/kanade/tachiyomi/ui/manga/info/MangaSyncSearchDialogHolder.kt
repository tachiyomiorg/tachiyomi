package eu.kanade.tachiyomi.ui.manga.info

import android.view.View
import eu.kanade.tachiyomi.data.database.models.MangaSync
import kotlinx.android.synthetic.main.dialog_manga_sync_search_result.view.*

/**
 * Holder that contains search items
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @constructor creates a new [MangaSyncSearchDialogHolder].
 */
class MangaSyncSearchDialogHolder(private val view: View) {

    /**
     * Set values of view
     * @param sync item containing [MangaSync] information
     */
    fun onSetValues(sync: MangaSync) {
        view.txt_manga_sync_result_title.text = sync.title
    }
}