package eu.kanade.tachiyomi.data.track.anilist.model

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.anilist.Anilist

data class ALUserManga(
        val id: Int,
        val list_status: String,
        val score_raw: Int,
        val chapters_read: Int,
        val manga: ALManga) {

    fun toTrack() = Track.create(TrackManager.ANILIST).apply {
        remote_id = manga.id
        status = toTrackStatus()
        score = score_raw.toFloat()
        last_chapter_read = chapters_read
    }

    fun toTrackStatus() = when (list_status) {
        "reading" -> Anilist.READING
        "completed" -> Anilist.COMPLETED
        "on-hold" -> Anilist.ON_HOLD
        "dropped" -> Anilist.DROPPED
        "plan to read" -> Anilist.PLAN_TO_READ
        else -> throw NotImplementedError("Unknown status")
    }
}