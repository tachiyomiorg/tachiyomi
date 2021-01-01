package eu.kanade.tachiyomi.ui.manga.chapter.base

import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page

abstract class BaseChapterItem<T : BaseChapterHolder, H : AbstractHeaderItem<*>>(
    val chapter: Chapter,
    header: H? = null
) :
    AbstractSectionableItem<T, H?>(header),
    Chapter by chapter {

    private var _status: Download.State = Download.State.NOT_DOWNLOADED

    var status: Download.State
        get() = download?.status ?: _status
        set(value) {
            _status = value
        }

    val progress: Int
        get() {
            val pages = download?.pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }

    @Transient
    var download: Download? = null

    val isDownloaded: Boolean
        get() = status == Download.State.DOWNLOADED

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is BaseChapterItem<*, *>) {
            return chapter.id!! == other.chapter.id!!
        }
        return false
    }

    override fun hashCode(): Int {
        return chapter.id!!.hashCode()
    }
}
