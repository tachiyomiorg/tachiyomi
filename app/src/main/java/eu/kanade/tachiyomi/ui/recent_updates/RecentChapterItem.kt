package eu.kanade.tachiyomi.ui.recent_updates

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download

class RecentChapterItem(val chapter: Chapter, val manga: Manga, header: DateItem) :
        AbstractSectionableItem<RecentChapterHolder, DateItem>(header) {

    private var _status: Int = 0

    var status: Int
        get() = download?.status ?: _status
        set(value) {
            _status = value
        }

    @Transient
    var download: Download? = null

    val isDownloaded: Boolean
        get() = status == Download.DOWNLOADED

    override fun getLayoutRes(): Int {
        return R.layout.recent_chapters_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): RecentChapterHolder {
        return RecentChapterHolder(view, adapter as RecentChaptersAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: RecentChapterHolder,
        position: Int,
        payloads: List<Any?>?
    ) {

        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is RecentChapterItem) {
            return chapter.id!! == other.chapter.id!!
        }
        return false
    }

    override fun hashCode(): Int {
        return chapter.id!!.hashCode()
    }
}
