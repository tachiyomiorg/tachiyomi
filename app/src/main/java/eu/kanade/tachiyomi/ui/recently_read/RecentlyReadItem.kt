package eu.kanade.tachiyomi.ui.recently_read

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.util.inflate

class RecentlyReadItem(val mch: MangaChapterHistory) : AbstractFlexibleItem<RecentlyReadHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.item_recently_read
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>,
                                  inflater: LayoutInflater,
                                  parent: ViewGroup): RecentlyReadHolder {

        val view = parent.inflate(layoutRes)
        return RecentlyReadHolder(view, adapter as RecentlyReadAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>,
                                holder: RecentlyReadHolder,
                                position: Int,
                                payloads: List<Any?>?) {

        holder.bind(mch)
    }

    override fun equals(other: Any?): Boolean {
        if (other is RecentlyReadItem) {
            return mch.manga.id == other.mch.manga.id
        }
        return false
    }

    override fun hashCode(): Int {
        return mch.manga.id!!.hashCode()
    }
}