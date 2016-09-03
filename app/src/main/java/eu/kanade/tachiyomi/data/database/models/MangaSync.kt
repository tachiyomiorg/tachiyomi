package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface MangaSync : Serializable {

    var id: Long?

    var manga_id: Long

    var sync_id: Int

    var remote_id: Int

    var remote_score: Float

    var title: String

    var last_chapter_read: Int

    var total_chapters: Int

    var score: Float

    var status: Int

    var update: Boolean

    var is_bind: Boolean

    fun copyPersonalFrom(other: MangaSync) {
        last_chapter_read = other.last_chapter_read
        score = other.score
        status = other.status
    }

    companion object {

        fun create(serviceId: Int): MangaSync = MangaSyncImpl().apply {
            sync_id = serviceId
        }
    }

}
