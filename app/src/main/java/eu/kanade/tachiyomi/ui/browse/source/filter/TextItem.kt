package eu.kanade.tachiyomi.ui.browse.source.filter

import android.view.View
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Filter

open class TextItem(val filter: Filter.Text) : AbstractFlexibleItem<TextItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.navigation_view_text
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): Holder {
        return Holder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: Holder, position: Int, payloads: List<Any?>?) {
        holder.wrapper.hint = filter.name
        holder.edit.setText(filter.state)
        holder.edit.doOnTextChanged { text, _, _, _ ->
            filter.state = text.toString()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return filter == (other as TextItem).filter
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        val wrapper: TextInputLayout = itemView.findViewById(R.id.nav_view_item_wrapper)
        val edit: EditText = itemView.findViewById(R.id.nav_view_item)
    }
}
