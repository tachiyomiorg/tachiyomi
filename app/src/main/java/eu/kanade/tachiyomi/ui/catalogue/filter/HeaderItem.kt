package eu.kanade.tachiyomi.ui.catalogue.filter

import android.annotation.SuppressLint
import android.support.design.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.source.model.Filter

class HeaderItem(val filter: Filter.Header) : AbstractHeaderItem<HeaderItem.Holder>() {

    @SuppressLint("PrivateResource")
    override fun getLayoutRes(): Int {
        return R.layout.design_navigation_item_subheader
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater, parent: ViewGroup): Holder {
        return Holder(inflater.inflate(layoutRes, parent, false), adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: Holder, position: Int, payloads: List<Any?>?) {
        val view = holder.itemView as TextView
        view.text = filter.name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is HeaderItem) {
            return filter == other.filter
        }
        return false
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter)
}
