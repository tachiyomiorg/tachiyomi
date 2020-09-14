package eu.kanade.tachiyomi.ui.browse.source.filter

import android.view.View
import android.widget.CheckedTextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.R as TR

open class TriStateItem(val filter: Filter.TriState) : AbstractFlexibleItem<TriStateItem.Holder>() {

    override fun getLayoutRes(): Int {
        return TR.layout.navigation_view_checkedtext
    }

    override fun getItemViewType(): Int {
        return 103
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): Holder {
        return Holder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: Holder, position: Int, payloads: List<Any?>?) {
        val view = holder.text
        view.text = filter.name

        fun getIcon() = AppCompatResources.getDrawable(
            view.context,
            when (filter.state) {
                Filter.TriState.STATE_IGNORE -> TR.drawable.ic_check_box_outline_blank_24dp
                Filter.TriState.STATE_INCLUDE -> TR.drawable.ic_check_box_24dp
                Filter.TriState.STATE_EXCLUDE -> TR.drawable.ic_check_box_x_24dp
                else -> throw Exception("Unknown state")
            }
        )?.apply {
            val color = if (filter.state == Filter.TriState.STATE_INCLUDE) {
                view.context.getResourceColor(R.attr.colorAccent)
            } else {
                view.context.getResourceColor(R.attr.colorOnBackground, 0.38f)
            }

            setTint(color)
        }

        view.setCompoundDrawablesWithIntrinsicBounds(getIcon(), null, null, null)
        holder.itemView.setOnClickListener {
            filter.state = (filter.state + 1) % 3
            view.setCompoundDrawablesWithIntrinsicBounds(getIcon(), null, null, null)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return filter == (other as TriStateItem).filter
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        val text: CheckedTextView = itemView.findViewById(TR.id.nav_view_item)

        init {
            // Align with native checkbox
            text.setPadding(4.dpToPx, 0, 0, 0)
            text.compoundDrawablePadding = 20.dpToPx
        }
    }
}
