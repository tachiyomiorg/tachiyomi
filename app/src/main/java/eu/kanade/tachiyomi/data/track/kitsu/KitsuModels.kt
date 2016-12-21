package eu.kanade.tachiyomi.data.track.kitsu

import android.support.annotation.CallSuper
import com.github.salomonbrys.kotson.*
import com.google.gson.JsonObject
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager

open class KitsuManga(obj: JsonObject) {
    val id by obj.byInt
    val canonicalTitle by obj["attributes"].byString
    val chapterCount = obj["attributes"]["chapterCount"].nullInt

    @CallSuper
    open fun toTrack() = Track.create(TrackManager.KITSU).apply {
        remote_id = this@KitsuManga.id
        title = canonicalTitle
        total_chapters = chapterCount ?: 0
    }
}

class KitsuLibManga(obj: JsonObject, manga: JsonObject) : KitsuManga(manga) {
    val remoteId by obj.byInt("id")
    val status by obj["attributes"].byString
    val rating = obj["attributes"]["rating"].nullString
    val progress by obj["attributes"].byInt

    override fun toTrack() = super.toTrack().apply {
        remote_id = remoteId
        status = toTrackStatus()
        score = rating?.let { it.toFloat() * 2 } ?: 0f
        last_chapter_read = progress
    }

    private fun toTrackStatus() = when (status) {
        "current" -> Kitsu.READING
        "completed" -> Kitsu.COMPLETED
        "on_hold" -> Kitsu.ON_HOLD
        "dropped" -> Kitsu.DROPPED
        "planned" -> Kitsu.PLAN_TO_READ
        else -> throw Exception("Unknown status")
    }

}
