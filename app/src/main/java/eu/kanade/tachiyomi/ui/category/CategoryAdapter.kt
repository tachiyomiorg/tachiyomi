package eu.kanade.tachiyomi.ui.category

import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Custom adapter for categories.
 *
 * @param controller The containing controller.
 */
class CategoryAdapter(controller: CategoryController) :
    FlexibleAdapter<CategoryItem>(null, controller, true) {

    /**
     * Listener called when an item of the list is released.
     */
    val onItemReleaseListener: OnItemReleaseListener = controller

    /**
     * Clears the active selections from the list and the model.
     */
    override fun clearSelection() {
        super.clearSelection()
        (0 until itemCount).forEach { getItem(it)?.isSelected = false }
    }

    /**
     * Toggles the selection of the given position.
     *
     * @param position The position to toggle.
     */
    override fun toggleSelection(position: Int) {
        super.toggleSelection(position)
        getItem(position)?.isSelected = isSelected(position)
    }

    interface OnItemReleaseListener {
        /**
         * Called when an item of the list is released.
         */
        fun onItemReleased(position: Int)
    }
}
