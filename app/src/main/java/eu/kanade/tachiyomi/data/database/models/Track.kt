package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Track : Serializable {

    var id: Long?

    var manga_id: Long

    var sync_id: Int

    var media_id: Int

    var library_id: Long?

    var title: String

    var last_chapter_read: Int

    var total_chapters: Int

    var score: Float

    var status: Int

    var tracking_url: String

    fun copyPersonalFrom(other: Track) {
        last_chapter_read = other.last_chapter_read
        score = other.score
        status = other.status
    }

    companion object {
        fun create(serviceId: Int): Track = TrackImpl().apply {
            sync_id = serviceId
        }
    }

}
