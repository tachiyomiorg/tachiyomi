package eu.kanade.tachiyomi.ui.category

import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Adapter of CategoryHolder.
 * Connection between Activity and Holder
 * Holder updates should be called from here.
 *
 * @param activity activity that created adapter
 * @constructor Creates a CategoryAdapter object
 */
class CategoryAdapter(private val activity: CategoryActivity) :
        FlexibleAdapter<CategoryItem>(null, activity, true) {

    /**
     * Called when item is released.
     */
    fun onItemReleased() {
        activity.onItemReleased()
    }

    override fun clearSelection() {
        super.clearSelection()
        (0..itemCount-1).forEach { getItem(it).isSelected = false }
    }

    override fun toggleSelection(position: Int) {
        super.toggleSelection(position)
        getItem(position).isSelected = isSelected(position)
    }

}
