package eu.kanade.tachiyomi.ui.catalogue

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R


class ProgressItem : AbstractFlexibleItem<ProgressItem.Holder>() {

    var loadMore = true

    override fun getLayoutRes(): Int {
        return R.layout.progress_item
    }

    override fun createViewHolder(adapter: FlexibleAdapter<IFlexible<*>>, inflater: LayoutInflater, parent: ViewGroup): Holder {
        return Holder(inflater.inflate(layoutRes, parent, false), adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>>, holder: Holder, position: Int, payloads: List<Any?>) {
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

        val progressBar = view.findViewById(R.id.progress_bar) as ProgressBar
        val progressMessage = view.findViewById(R.id.progress_message) as TextView
    }

}