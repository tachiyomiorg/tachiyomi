package eu.kanade.tachiyomi.ui.catalogue.filter

import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.util.getResourceColor

class SortItem(val name: String, val group: SortGroup) : AbstractSectionableItem<SortItem.Holder, SortGroup>(group) {

    // Use an id instead of the layout res to allow to reuse the layout.
    override fun getLayoutRes(): Int {
        return R.id.catalogue_filter_sort_item
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater, parent: ViewGroup): Holder {
        return Holder(inflater.inflate(R.layout.navigation_view_checkedtext, parent, false), adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: Holder, position: Int, payloads: List<Any?>?) {
        val view = holder.text
        view.text = name
        val filter = group.filter

        val i = filter.values.indexOf(name)

        fun getIcon() = when (filter.state) {
            Filter.Sort.Selection(i, false) -> VectorDrawableCompat.create(view.resources, R.drawable.ic_keyboard_arrow_down_black_32dp, null)
                    ?.apply { setTint(view.context.getResourceColor(R.attr.colorAccent)) }
            Filter.Sort.Selection(i, true) -> VectorDrawableCompat.create(view.resources, R.drawable.ic_keyboard_arrow_up_black_32dp, null)
                    ?.apply { setTint(view.context.getResourceColor(R.attr.colorAccent)) }
            else -> ContextCompat.getDrawable(view.context, R.drawable.empty_drawable_32dp)
        }

        view.setCompoundDrawablesWithIntrinsicBounds(getIcon(), null, null, null)
        holder.itemView.setOnClickListener {
            val pre = filter.state?.index ?: i
            if (pre != i) {
                filter.state = Filter.Sort.Selection(i, false)
            } else {
                filter.state = Filter.Sort.Selection(i, filter.state?.ascending == false)
            }

            group.subItems.forEach { adapter.notifyItemChanged(adapter.getGlobalPositionOf(it)) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is SortItem) {
            return name == other.name && group == other.group
        }
        return false
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + group.hashCode()
        return result
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {

        val text = itemView.findViewById(R.id.nav_view_item) as CheckedTextView
    }

}