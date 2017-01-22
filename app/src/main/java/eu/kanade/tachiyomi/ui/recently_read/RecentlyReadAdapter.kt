package eu.kanade.tachiyomi.ui.recently_read

import android.view.ViewGroup
import eu.davidea.flexibleadapter4.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.inflate
import uy.kohesive.injekt.injectLazy

/**
 * Adapter of RecentlyReadHolder.
 * Connection between Fragment and Holder
 * Holder updates should be called from here.
 *
 * @param fragment a RecentlyReadFragment object
 * @constructor creates an instance of the adapter.
 */
class RecentlyReadAdapter(val fragment: RecentlyReadFragment)
: FlexibleAdapter<RecentlyReadHolder, MangaChapterHistory>() {

    val sourceManager by injectLazy<SourceManager>()

    /**
     * Called when ViewHolder is created
     * @param parent parent View
     * @param viewType int containing viewType
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentlyReadHolder {
        val view = parent.inflate(R.layout.item_recently_read)
        return RecentlyReadHolder(view, this)
    }

    /**
     * Called when ViewHolder is bind
     * @param holder bind holder
     * @param position position of holder
     */
    override fun onBindViewHolder(holder: RecentlyReadHolder, position: Int) {
        val item = getItem(position)
        holder.onSetValues(item)
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
