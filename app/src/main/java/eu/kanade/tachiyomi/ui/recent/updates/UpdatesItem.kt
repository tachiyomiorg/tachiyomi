package eu.kanade.tachiyomi.ui.recent.updates

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.manga.chapter.base.BaseChapterItem
import eu.kanade.tachiyomi.ui.recent.DateSectionItem

class UpdatesItem(chapter: Chapter, val manga: Manga, header: DateSectionItem) :
    BaseChapterItem<UpdatesHolder, DateSectionItem>(chapter, header) {

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
}
