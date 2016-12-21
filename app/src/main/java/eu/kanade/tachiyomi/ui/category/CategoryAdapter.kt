package eu.kanade.tachiyomi.ui.category

import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.adapter.ItemTouchHelperAdapter
import eu.kanade.tachiyomi.util.inflate
import java.util.*

/**
 * Adapter of CategoryHolder.
 * Connection between Activity and Holder
 * Holder updates should be called from here.
 *
 * @param activity activity that created adapter
 * @constructor Creates a CategoryAdapter object
 */
class CategoryAdapter(private val activity: CategoryActivity) :
        FlexibleAdapter<CategoryHolder, Category>(), ItemTouchHelperAdapter {

    init {
        // Set unique id's
        setHasStableIds(true)
    }

    /**
     * Called when ViewHolder is created
     *
     * @param parent parent View
     * @param viewType int containing viewType
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryHolder {
        // Inflate layout with item_edit_categories.xml
        val view = parent.inflate(R.layout.item_edit_categories)
        return CategoryHolder(view, this, activity, activity)
    }

    /**
     * Called when ViewHolder is bind
     *
     * @param holder bind holder
     * @param position position of holder
     */
    override fun onBindViewHolder(holder: CategoryHolder, position: Int) {
        // Update holder values.
        val category = getItem(position)
        holder.onSetValues(category)

        //When user scrolls this bind the correct selection status
        holder.itemView.isActivated = isSelected(position)
    }

    /**
     * Update items with list of categories
     *
     * @param items list of categories
     */
    fun setItems(items: List<Category>) {
        mItems = ArrayList(items)
        notifyDataSetChanged()
    }

    /**
     * Get category by position
     *
     * @param position position of item
     */
    override fun getItemId(position: Int): Long {
        return mItems[position].id!!.toLong()
    }

    /**
     * Called when item is moved
     *
     * @param fromPosition previous position of item.
     * @param toPosition new position of item.
     */
    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        // Move items and notify touch helper
        Collections.swap(mItems, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)

        // Update database
        activity.presenter.reorderCategories(mItems)
    }

    /**
     * Must be implemented, not used
     */
    override fun onItemDismiss(position: Int) {
        // Empty method.
    }

    /**
     * Must be implemented, not used
     */
    override fun updateDataSet(p0: String?) {
        // Empty method.
    }
}
