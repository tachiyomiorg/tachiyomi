package eu.kanade.tachiyomi.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.util.inflate

/**
 * Category item for a recycler view.
 */
class CategoryItem(val category: Category) : AbstractFlexibleItem<CategoryHolder>() {

    /**
     * Whether this item is currently selected.
     */
    var isSelected = false

    /**
     * Returns the layout resource for this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.categories_item
    }

    /**
     * Returns a new view holder for this item.
     *
     * @param adapter The adapter of this item.
     * @param inflater The layout inflater for XML inflation.
     * @param parent The container view.
     */
    override fun createViewHolder(adapter: FlexibleAdapter<*>,
                                  inflater: LayoutInflater,
                                  parent: ViewGroup): CategoryHolder {

        return CategoryHolder(parent.inflate(layoutRes), adapter as CategoryAdapter)
    }

    /**
     * Binds the given view holder with this item.
     *
     * @param adapter The adapter of this item.
     * @param holder The holder to bind.
     * @param position The position of this item in the adapter.
     * @param payloads List of partial changes.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>,
                                holder: CategoryHolder,
                                position: Int,
                                payloads: List<Any?>?) {

        holder.bind(category)
    }

    /**
     * Returns true if this item is draggable.
     */
    override fun isDraggable(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is CategoryItem) {
            return category.id == other.category.id
        }
        return false
    }

    override fun hashCode(): Int {
        return category.id!!
    }

}