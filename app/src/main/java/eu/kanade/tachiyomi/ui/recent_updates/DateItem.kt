package eu.kanade.tachiyomi.ui.recent_updates

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import java.util.*

class DateItem(val date: Date) : AbstractHeaderItem<DateItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.recent_chapters_section_item
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater, parent: ViewGroup): Holder {
        return Holder(inflater.inflate(layoutRes, parent, false), adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: Holder, position: Int, payloads: List<Any?>?) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is DateItem) {
            return date == other.date
        }
        return false
    }

    override fun hashCode(): Int {
        return date.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter, true) {

        private val now = Date().time

        val section_text = view.findViewById(R.id.section_text) as TextView

        fun bind(item: DateItem) {
            section_text.text = DateUtils.getRelativeTimeSpanString(item.date.time, now, DateUtils.DAY_IN_MILLIS)
        }
    }
}