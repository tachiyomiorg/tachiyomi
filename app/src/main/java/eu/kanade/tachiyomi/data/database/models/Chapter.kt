package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Chapter : Serializable {

    var id: Long?

    var manga_id: Long?

    var url: String

    var name: String

    var read: Boolean

    var bookmark: Boolean

    var last_page_read: Int

    var date_fetch: Long

    var date_upload: Long

    var chapter_number: Float

    var source_order: Int

    val isRecognizedNumber: Boolean
        get() = chapter_number >= 0f

    companion object {

        fun create(): Chapter = ChapterImpl().apply {
            chapter_number = -1f
        }
    }
}
