package eu.kanade.tachiyomi.ui.catalogue.filter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener

open class SelectItem(val filter: Filter.Select<*>) : AbstractFlexibleItem<SelectItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.navigation_view_spinner
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater, parent: ViewGroup): Holder {
        return Holder(inflater.inflate(layoutRes, parent, false), adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: Holder, position: Int, payloads: List<Any?>?) {
        holder.text.text = filter.name + ": "

        val spinner = holder.spinner
        spinner.prompt = filter.name
        spinner.adapter = ArrayAdapter<Any>(holder.itemView.context,
                android.R.layout.simple_spinner_item, filter.values).apply {
            setDropDownViewResource(R.layout.spinner_item)
        }
        spinner.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            filter.state = position
        }
        spinner.setSelection(filter.state)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is SelectItem) {
            return filter == other.filter
        }
        return false
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {

        val text = itemView.findViewById(R.id.nav_view_item_text) as TextView
        val spinner = itemView.findViewById(R.id.nav_view_item) as Spinner
    }
}