package eu.kanade.tachiyomi.data.track.kitsu

import android.support.annotation.CallSuper
import com.github.salomonbrys.kotson.*
import com.google.gson.JsonObject
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch

open class KitsuManga(obj: JsonObject) {
    val id by obj.byInt
    val canonicalTitle by obj["attributes"].byString
    val chapterCount = obj["attributes"].obj.get("chapterCount").nullInt
    val type = obj["attributes"].obj.get("mangaType").nullString.orEmpty()
    val original by obj["attributes"].obj["posterImage"].byString
    val synopsis by obj["attributes"].byString
    val startDate = obj["attributes"].obj.get("startDate").nullString.orEmpty()
    open val status = obj["attributes"].obj.get("status").nullString.orEmpty()

    @CallSuper
    open fun toTrack() = TrackSearch.create(TrackManager.KITSU).apply {
        media_id = this@KitsuManga.id
        title = canonicalTitle
        total_chapters = chapterCount ?: 0
        cover_url = original
        summary = synopsis
        tracking_url = KitsuApi.mangaUrl(media_id)
        publishing_status = this@KitsuManga.status
        publishing_type = type
        start_date = startDate.orEmpty()
    }
}

class KitsuLibManga(obj: JsonObject, manga: JsonObject) : KitsuManga(manga) {
    val libraryId by obj.byInt("id")
    override val status by obj["attributes"].byString
    val ratingTwenty = obj["attributes"].obj.get("ratingTwenty").nullString
    val progress by obj["attributes"].byInt

    override fun toTrack() = super.toTrack().apply {
        media_id = libraryId // TODO migrate media ids to library ids
        status = toTrackStatus()
        score = ratingTwenty?.let { it.toInt() / 2f } ?: 0f
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

fun Track.toKitsuStatus() = when (status) {
    Kitsu.READING -> "current"
    Kitsu.COMPLETED -> "completed"
    Kitsu.ON_HOLD -> "on_hold"
    Kitsu.DROPPED -> "dropped"
    Kitsu.PLAN_TO_READ -> "planned"
    else -> throw Exception("Unknown status")
}

fun Track.toKitsuScore(): String? {
    return if (score > 0) (score * 2).toInt().toString() else null
}
