package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.manga.chapter.base.BaseChapterItem

class ChapterItem(chapter: Chapter, val manga: Manga) :
    BaseChapterItem<ChapterHolder, AbstractHeaderItem<FlexibleViewHolder>>(chapter) {

    override fun getLayoutRes(): Int {
        return R.layout.chapters_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ChapterHolder {
        return ChapterHolder(view, adapter as ChaptersAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: ChapterHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.bind(this, manga)
    }
}
