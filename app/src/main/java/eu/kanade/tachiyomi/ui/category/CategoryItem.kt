package eu.kanade.tachiyomi.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.util.inflate

class CategoryItem(val category: Category) : AbstractFlexibleItem<CategoryHolder>() {

    var isSelected = false

    override fun getLayoutRes(): Int {
        return R.layout.item_edit_categories
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CategoryHolder {
        return CategoryHolder(parent.inflate(layoutRes), adapter as CategoryAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CategoryHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(category)
    }

    override fun isDraggable(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (other is CategoryItem) {
            return category.id == other.category.id
        }
        return false
    }

    override fun hashCode(): Int {
        return category.id!!
    }

}