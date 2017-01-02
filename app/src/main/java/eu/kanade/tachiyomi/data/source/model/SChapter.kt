package eu.kanade.tachiyomi.data.source.model

import java.io.Serializable

interface SChapter : Serializable {

    var url: String

    var name: String

    // Remove?
    var date_fetch: Long

    var date_upload: Long

    var chapter_number: Float

    var source_order: Int

    companion object {
        fun create(): SChapter {
            return SChapterImpl()
        }
    }

}