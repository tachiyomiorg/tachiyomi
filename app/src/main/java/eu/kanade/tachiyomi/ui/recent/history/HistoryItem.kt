package eu.kanade.tachiyomi.ui.recent.history

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.ui.recent.DateSectionItem

class HistoryItem(val mch: MangaChapterHistory, header: DateSectionItem) :
    AbstractSectionableItem<HistoryHolder, DateSectionItem>(header) {

    override fun getLayoutRes(): Int {
        return R.layout.history_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): HistoryHolder {
        return HistoryHolder(view, adapter as HistoryAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: HistoryHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.bind(mch)
    }

    override fun equals(other: Any?): Boolean {
        if (other is HistoryItem) {
            return mch.manga.id == other.mch.manga.id
        }
        return false
    }

    override fun hashCode(): Int {
        return mch.manga.id!!.hashCode()
    }
}
