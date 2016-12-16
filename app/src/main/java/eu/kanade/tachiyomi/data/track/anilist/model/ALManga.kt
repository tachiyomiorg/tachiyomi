package eu.kanade.tachiyomi.data.track.anilist.model

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager

data class ALManga(
        val id: Int,
        val title_romaji: String,
        val type: String,
        val total_chapters: Int) {

    fun toTrack() = Track.create(TrackManager.ANILIST).apply {
        remote_id = this@ALManga.id
        title = title_romaji
        total_chapters = this@ALManga.total_chapters
    }
}