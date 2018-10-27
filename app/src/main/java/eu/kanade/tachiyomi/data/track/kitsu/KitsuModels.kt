package eu.kanade.tachiyomi.data.track.kitsu

import android.support.annotation.CallSuper
import com.github.salomonbrys.kotson.*
import com.google.gson.JsonObject
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import java.text.SimpleDateFormat
import java.util.*

class KitsuSearchManga(obj: JsonObject) {
    val id by obj.byInt
    private val canonicalTitle by obj.byString
    private val chapterCount = obj.get("chapterCount").nullInt
    val subType = obj.get("subtype").nullString
    val original = obj.get("posterImage").nullObj?.get("original")?.asString
    private val synopsis by obj.byString
    private var startDate = obj.get("startDate").nullString?.let {
        val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        outputDf.format(Date(it!!.toLong() * 1000))
    }
    private val endDate = obj.get("endDate").nullString


    @CallSuper
    open fun toTrack() = TrackSearch.create(TrackManager.KITSU).apply {
        media_id = this@KitsuSearchManga.id
        title = canonicalTitle
        total_chapters = chapterCount ?: 0
        cover_url = original ?: ""
        summary = synopsis
        tracking_url = KitsuApi.mangaUrl(media_id)
        if (endDate == null) {
            publishing_status = "Publishing"
        } else {
            publishing_status = "Finished"
        }
        publishing_type = subType ?: ""
        start_date = startDate ?: ""
    }
}


class KitsuLibManga(obj: JsonObject, manga: JsonObject) {
    val id by manga.byInt
    private val canonicalTitle by manga["attributes"].byString
    private val chapterCount = manga["attributes"].obj.get("chapterCount").nullInt
    val type = manga["attributes"].obj.get("mangaType").nullString.orEmpty()
    val original by manga["attributes"].obj["posterImage"].byString
    private val synopsis by manga["attributes"].byString
    private val startDate = manga["attributes"].obj.get("startDate").nullString.orEmpty()
    private val libraryId by obj.byInt("id")
    val status by obj["attributes"].byString
    private val ratingTwenty = obj["attributes"].obj.get("ratingTwenty").nullString
    val progress by obj["attributes"].byInt

    open fun toTrack() = TrackSearch.create(TrackManager.KITSU).apply {
        media_id = libraryId
        title = canonicalTitle
        total_chapters = chapterCount ?: 0
        cover_url = original
        summary = synopsis
        tracking_url = KitsuApi.mangaUrl(media_id)
        publishing_status = this@KitsuLibManga.status
        publishing_type = type
        start_date = startDate
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
