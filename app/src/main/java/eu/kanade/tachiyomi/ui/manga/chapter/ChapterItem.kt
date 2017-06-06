package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download

class ChapterItem(val chapter: Chapter, val manga: Manga) : AbstractFlexibleItem<ChapterHolder>(),
        Chapter by chapter {

    private var _status: Int = 0

    var status: Int
        get() = download?.status ?: _status
        set(value) { _status = value }

    @Transient var download: Download? = null

    val isDownloaded: Boolean
        get() = status == Download.DOWNLOADED

    override fun getLayoutRes(): Int {
        return R.layout.chapters_item
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>,
                                  inflater: LayoutInflater,
                                  parent: ViewGroup): ChapterHolder {

        return ChapterHolder(inflater.inflate(layoutRes, parent, false), adapter as ChaptersAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>,
                                holder: ChapterHolder,
                                position: Int,
                                payloads: List<Any?>?) {

        holder.bind(this, manga)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ChapterItem) {
            return chapter.id!! == other.chapter.id!!
        }
        return false
    }

    override fun hashCode(): Int {
        return chapter.id!!.hashCode()
    }

}