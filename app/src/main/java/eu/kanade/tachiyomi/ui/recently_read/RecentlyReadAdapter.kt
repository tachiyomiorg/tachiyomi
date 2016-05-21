package eu.kanade.tachiyomi.ui.recently_read

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.util.inflate

/**
 * Adapter of RecentlyReadHolder.
 * Connection between Fragment and Holder
 * Holder updates should be called from here.
 *
 * @param fragment a RecentlyReadFragment object
 * @constructor creates an instance of the adapter.
 */
class RecentlyReadAdapter(val fragment: RecentlyReadFragment) : FlexibleAdapter<RecyclerView.ViewHolder, Any>() {
    /**
     * Called when ViewHolder is created
     * @param parent parent View
     * @param viewType int containing viewType
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
        val view = parent.inflate(R.layout.item_recent_manga)
        return RecentlyReadHolder(view, this)
    }

    /**
     * Called when ViewHolder is bind
     * @param holder bind holder
     * @param position position of holder
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        val item = getItem(position) as MangaChapterHistory
        (holder as RecentlyReadHolder).onSetValues(item)
    }

    /**
     * Update items
     * @param items items
     */
    fun setItems(items: List<MangaChapterHistory>) {
        mItems = items
        notifyDataSetChanged()
    }

    override fun updateDataSet(param: String?) {
        // Empty function
    }

}
