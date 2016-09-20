package eu.kanade.tachiyomi.ui.catalogue

import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.fragment_catalogue.*
import kotlinx.android.synthetic.main.item_catalogue_grid.view.*
import java.util.*

/**
 * Adapter storing a list of manga from the catalogue.
 *
 * @param fragment the fragment containing this adapter.
 */
class CatalogueAdapter(val fragment: CatalogueFragment) : FlexibleAdapter<CatalogueHolder, Manga>() {

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
     *
     * @param list the list to add.
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
     *
     * @param position the position in the adapter.
     * @return an identifier for the item.
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
     *
     * @param parent the parent view.
     * @param viewType the type of the holder.
     * @return a new view holder for a manga.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogueHolder {
        if (parent.id == R.id.catalogue_grid) {
            val view = parent.inflate(R.layout.item_catalogue_grid).apply {
                card.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight)
                gradient.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight / 2, Gravity.BOTTOM)
            }
            return CatalogueGridHolder(view, this, fragment)
        } else {
            val view = parent.inflate(R.layout.item_catalogue_list)
            return CatalogueListHolder(view, this, fragment)
        }
    }

    /**
     * Binds a holder with a new position.
     *
     * @param holder the holder to bind.
     * @param position the position to bind.
     */
    override fun onBindViewHolder(holder: CatalogueHolder, position: Int) {
        val manga = getItem(position)
        holder.onSetValues(manga)
    }

    /**
     * Property to return the height for the covers based on the width to keep an aspect ratio.
     */
    val coverHeight: Int
        get() = fragment.catalogue_grid.itemWidth / 3 * 4

}
