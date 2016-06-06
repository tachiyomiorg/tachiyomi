package eu.kanade.tachiyomi.ui.manga.info

import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.util.inflate

/**
 * Adapter of [MangaSyncHolder].
 * Connection between Fragment and Holder
 * Holder updates should be called from here.
 *
 * @param fragment a RecentlyReadFragment object
 * @constructor creates an instance of the adapter.
 */
class MangaSyncAdapter(val fragment: MangaInfoFragment) : FlexibleAdapter<MangaSyncHolder, MangaSync>() {

    /**
     * Called when ViewHolder is created
     * @param parent parent View
     * @param viewType int containing viewType
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaSyncHolder {
        val view = parent.inflate(R.layout.item_manga_sync)
        return MangaSyncHolder(view, this)
    }

    /**
     * Called when ViewHolder is bind
     * @param holder bind holder
     * @param position position of holder
     */
    override fun onBindViewHolder(holder: MangaSyncHolder, position: Int) {
        holder.onSetValues(getItem(position))
    }

    /**
     * Update items
     * @param items items
     */
    fun setItems(items: List<MangaSync>) {
        mItems = items
        notifyDataSetChanged()
    }

    override fun updateDataSet(param: String?) {
        // Empty function
    }

}