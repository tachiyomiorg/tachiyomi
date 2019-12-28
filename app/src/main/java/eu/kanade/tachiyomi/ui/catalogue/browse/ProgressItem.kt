package eu.kanade.tachiyomi.ui.catalogue.browse

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R


class ProgressItem : AbstractFlexibleItem<ProgressItem.Holder>() {

    private var loadMore = true

    override fun getLayoutRes(): Int {
        return R.layout.catalogue_progress_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): Holder {
        return Holder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: Holder, position: Int, payloads: List<Any?>) {
        holder.progressBar.visibility = View.GONE
        holder.progressMessage.visibility = View.GONE

        if (!adapter.isEndlessScrollEnabled) {
            loadMore = false
        }

        if (loadMore) {
            holder.progressBar.visibility = View.VISIBLE
        } else {
            holder.progressMessage.visibility = View.VISIBLE
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {

        val progressBar: ProgressBar = view.findViewById(R.id.progress_bar)
        val progressMessage: TextView = view.findViewById(R.id.progress_message)
    }

}
