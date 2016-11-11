package eu.kanade.tachiyomi.ui.category

import eu.kanade.tachiyomi.ui.base.adapter.ItemTouchHelperAdapter
import eu.kanade.tachiyomi.ui.base.adapter.SimpleItemTouchHelperCallback

class CategoryItemTouchHelper(adapter: ItemTouchHelperAdapter) : SimpleItemTouchHelperCallback(adapter) {

    /**
     * Disable items swipe remove
     *
     * @return false
     */
    override fun isItemViewSwipeEnabled() = false

    /**
     * Disable long press item drag
     *
     * @return false
     */
    override fun isLongPressDragEnabled() = false

}