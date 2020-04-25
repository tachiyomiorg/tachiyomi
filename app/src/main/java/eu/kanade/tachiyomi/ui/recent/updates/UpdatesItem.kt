package eu.kanade.tachiyomi.ui.recent.updates

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.recent.DateSectionItem

class UpdatesItem(val chapter: Chapter, val manga: Manga, header: DateSectionItem) :
    AbstractSectionableItem<UpdatesHolder, DateSectionItem>(header) {

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
        return R.layout.updates_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): UpdatesHolder {
        return UpdatesHolder(view, adapter as UpdatesAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: UpdatesHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is UpdatesItem) {
            return chapter.id!! == other.chapter.id!!
        }
        return false
    }

    override fun hashCode(): Int {
        return chapter.id!!.hashCode()
    }
}
