package eu.kanade.tachiyomi.ui.latest_updates

import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.fragment_latest_updates.*
import kotlinx.android.synthetic.main.item_catalogue_grid.view.*
import java.util.*

/**
 * Adapter storing a list of manga from the catalogue.
 */
class LatestUpdatesAdapter(val fragment: LatestUpdatesFragment) : FlexibleAdapter<LatestUpdatesHolder, Manga>() {

    /**
     * Property to get the list of manga in the adapter.
     */
    val items: List<Manga>
        get() = mItems

    init {
        mItems = ArrayList()
        setHasStableIds(true)
    }

    /**
     * Adds a list of manga to the adapter.
     */
    fun addItems(list: List<Manga>) {
        if (list.isNotEmpty()) {
            val sizeBeforeAdding = mItems.size
            mItems.addAll(list)
            notifyItemRangeInserted(sizeBeforeAdding, list.size)
        }
    }

    /**
     * Clears the list of manga from the adapter.
     */
    fun clear() {
        val sizeBeforeRemoving = mItems.size
        mItems.clear()
        notifyItemRangeRemoved(0, sizeBeforeRemoving)
    }

    /**
     * Returns the identifier for a manga.
     */
    override fun getItemId(position: Int): Long {
        return mItems[position].id!!
    }

    /**
     * Used to filter the list. Required but not used.
     */
    override fun updateDataSet(param: String) {}

    /**
     * Creates a new view holder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LatestUpdatesHolder {
        if (parent.id == R.id.latest_updates_grid) {
            val view = parent.inflate(R.layout.item_catalogue_grid).apply {
                card.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight)
                gradient.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight / 2, Gravity.BOTTOM)
            }
            return LatestUpdatesGridHolder(view, this, fragment)
        } else {
            val view = parent.inflate(R.layout.item_catalogue_list)
            return LatestUpdatesListHolder(view, this, fragment)
        }
    }

    /**
     * Binds a holder with a new position.
     */
    override fun onBindViewHolder(holder: LatestUpdatesHolder, position: Int) {
        val manga = getItem(position)
        holder.onSetValues(manga)
    }

    /**
     * Property to return the height for the covers based on the width to keep an aspect ratio.
     */
    val coverHeight: Int
        get() = fragment.latest_updates_grid.itemWidth / 3 * 4

}
