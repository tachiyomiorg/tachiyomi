package eu.kanade.tachiyomi.ui.browse.source.filter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.radiobutton.MaterialRadioButton
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.*
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.R as TR

open class RadioItem(val name: String, private val radioGroup: RadioGroupItem) : AbstractSectionableItem<RadioItem.Holder, RadioGroupItem>(radioGroup) {

    override fun getLayoutRes(): Int {
        return TR.layout.navigation_view_radio
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): Holder {
        return Holder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: Holder, position: Int, payloads: List<Any?>?) {
        val filter = radioGroup.filter

        val i = filter.values.indexOf(name)

        val view = holder.text
        view.text = filter.values[i]
        view.isChecked = filter.state == i

        // view.setCompoundDrawablesWithIntrinsicBounds(getIcon(), null, null, null)
        holder.itemView.setOnClickListener {
            filter.state = i
            radioGroup.subItems.forEach { adapter.notifyItemChanged(adapter.getGlobalPositionOf(it)) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RadioItem
        return name == other.name && radioGroup.filter == other.radioGroup.filter
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + radioGroup.hashCode()
        return result
    }

    open class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        val text: MaterialRadioButton = itemView.findViewById(TR.id.nav_view_item)

        init {
            // Align with native checkbox
            text.setPadding(4.dpToPx, 0, 0, 0)
            text.compoundDrawablePadding = 20.dpToPx
        }
    }
}
