package eu.kanade.tachiyomi.ui.browse.source.browse

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R

class ProgressItem : AbstractFlexibleItem<ProgressItem.Holder>() {

    private var loadMore = true

    override fun getLayoutRes(): Int {
        return R.layout.source_progress_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): Holder {
        return Holder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: Holder, position: Int, payloads: List<Any?>) {
        holder.progressBar.isVisible = false
        holder.progressMessage.isVisible = false

        if (!adapter.isEndlessScrollEnabled) {
            loadMore = false
        }

        if (loadMore) {
            holder.progressBar.isVisible = true
        } else {
            holder.progressMessage.isVisible = true
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return loadMore.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {

        val progressBar: ProgressBar = view.findViewById(R.id.progress_bar)
        val progressMessage: TextView = view.findViewById(R.id.progress_message)
    }
}
