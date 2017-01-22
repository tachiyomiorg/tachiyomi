package eu.kanade.tachiyomi.ui.catalogue.filter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Filter

open class CheckboxItem(val filter: Filter.CheckBox) : AbstractFlexibleItem<CheckboxItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.navigation_view_checkbox
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater, parent: ViewGroup): Holder {
        return Holder(inflater.inflate(layoutRes, parent, false), adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: Holder, position: Int, payloads: List<Any?>?) {
        val view = holder.check
        view.text = filter.name
        view.isChecked = filter.state
        holder.itemView.setOnClickListener {
            view.toggle()
            filter.state = view.isChecked
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is CheckboxItem) {
            return filter == other.filter
        }
        return false
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {

        val check = itemView.findViewById(R.id.nav_view_item) as CheckBox
    }
}