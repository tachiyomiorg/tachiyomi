package eu.kanade.tachiyomi.ui.recent.manga

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.inflate

class RecentMangaAdapter(val fragment: RecentMangaFragment) : FlexibleAdapter<RecyclerView.ViewHolder, Any>() {
    /**
     * Called when ViewHolder is created
     * @param parent parent View
     * @param viewType int containing viewType
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
        val view = parent.inflate(R.layout.item_recent_manga)
        return RecentMangaHolder(view, this)
    }

    /**
     * Called when ViewHolder is bind
     * @param holder bind holder
     * @param position position of holder
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        val item = getItem(position) as Manga
        (holder as RecentMangaHolder).onSetValues(item)
    }

    /**
     * Update items
     * @param items items
     */
    fun setItems(items: List<Manga>) {
        mItems = items
        notifyDataSetChanged()
    }

    override fun updateDataSet(param: String?) {
        // Empty function
    }

}