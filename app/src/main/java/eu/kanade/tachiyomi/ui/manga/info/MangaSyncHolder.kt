package eu.kanade.tachiyomi.ui.manga.info

import android.support.v7.widget.RecyclerView
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.util.setTextAppearanceCompat
import kotlinx.android.synthetic.main.item_manga_sync.view.*
import uy.kohesive.injekt.injectLazy

/**
 * Holder that contains manga sync item
 * Uses [R.layout.item_manga_sync]
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new [MangaSyncHolder].
 */
class MangaSyncHolder(view: View, private val adapter: MangaSyncAdapter)
: RecyclerView.ViewHolder(view) {

    /**
     * Provides operations to manage [MangaSync] objects through its interfaces
     */
    val syncManager: MangaSyncManager by injectLazy()

    /**
     * Set values of view
     * @param mangaSync item containing [MangaSync] information
     */
    fun onSetValues(mangaSync: MangaSync) {
        if (mangaSync.is_bind) {
            itemView.txt_bind_title.text = syncManager.getService(mangaSync.sync_id).name
            itemView.txt_bind_manga.text = mangaSync.title
            itemView.btn_bind.text = "Unbind"
            itemView.btn_bind.setTextAppearanceCompat(R.style.TextAppearance_Medium_Button_Negative)
            itemView.btn_bind.setOnClickListener() {
                unbindMangaSync(mangaSync, false)
            }
        } else {
            unbindMangaSync(mangaSync)
        }
    }

    /**
     * Called to remove [MangaSync] object from database
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     * @param onlyView set false to unbind manga in database
     */
    fun unbindMangaSync(mangaSync: MangaSync, onlyView: Boolean = true) {
        itemView.txt_bind_title.text = syncManager.getService(mangaSync.sync_id).name
        itemView.txt_bind_manga.text = "Not bound"
        itemView.btn_bind.text = "Bind"
        itemView.btn_bind.setTextAppearanceCompat(R.style.TextAppearance_Medium_Button)
        itemView.btn_bind.setOnClickListener() {
            adapter.fragment.showSearchDialog(mangaSync.sync_id)
        }

        if (adapter.fragment.isDefaultService(mangaSync))
            adapter.fragment.setEditViewVisibility(false)

        if (!onlyView) {
            adapter.fragment.unbindMangaSync(mangaSync)
        }
    }

}