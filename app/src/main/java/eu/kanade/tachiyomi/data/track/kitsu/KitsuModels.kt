package eu.kanade.tachiyomi.data.track.kitsu

import androidx.annotation.CallSuper
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KitsuSearchManga(obj: JsonObject) {
    val id = obj["id"]!!.jsonPrimitive.int
    private val canonicalTitle = obj["canonicalTitle"]!!.jsonPrimitive.content
    private val chapterCount = obj["chapterCount"]?.jsonPrimitive?.intOrNull
    val subType = obj["subtype"]?.jsonPrimitive?.contentOrNull
    val original = try {
        obj["posterImage"]?.jsonObject?.get("original")?.jsonPrimitive?.content
    } catch (e: IllegalArgumentException) {
        // posterImage is sometimes a jsonNull object instead
        null
    }
    private val synopsis = obj["synopsis"]!!.jsonPrimitive.content
    private var startDate = obj["startDate"]?.jsonPrimitive?.contentOrNull?.let {
        val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        outputDf.format(Date(it.toLong() * 1000))
    }
    private val endDate = obj["endDate"]?.jsonPrimitive?.contentOrNull

    @CallSuper
    fun toTrack() = TrackSearch.create(TrackManager.KITSU).apply {
        media_id = this@KitsuSearchManga.id
        title = canonicalTitle
        total_chapters = chapterCount ?: 0
        cover_url = original ?: ""
        summary = synopsis
        tracking_url = KitsuApi.mangaUrl(media_id)
        publishing_status = if (endDate == null) {
            "Publishing"
        } else {
            "Finished"
        }
        publishing_type = subType ?: ""
        start_date = startDate ?: ""
    }
}

class KitsuLibManga(obj: JsonObject, manga: JsonObject) {
    val id = manga["id"]!!.jsonPrimitive.int
    private val canonicalTitle = manga["attributes"]!!.jsonObject["canonicalTitle"]!!.jsonPrimitive.content
    private val chapterCount = manga["attributes"]!!.jsonObject["chapterCount"]?.jsonPrimitive?.intOrNull
    val type = manga["attributes"]!!.jsonObject["mangaType"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val original = manga["attributes"]!!.jsonObject["posterImage"]!!.jsonObject["original"]!!.jsonPrimitive.content
    private val synopsis = manga["attributes"]!!.jsonObject["synopsis"]!!.jsonPrimitive.content
    private val startDate = manga["attributes"]!!.jsonObject["startDate"]?.jsonPrimitive?.contentOrNull.orEmpty()
    private val startedAt = obj["attributes"]!!.jsonObject["startedAt"]?.jsonPrimitive?.contentOrNull
    private val finishedAt = obj["attributes"]!!.jsonObject["finishedAt"]?.jsonPrimitive?.contentOrNull
    private val libraryId = obj["id"]!!.jsonPrimitive.int
    val status = obj["attributes"]!!.jsonObject["status"]!!.jsonPrimitive.content
    private val ratingTwenty = obj["attributes"]!!.jsonObject["ratingTwenty"]?.jsonPrimitive?.contentOrNull
    val progress = obj["attributes"]!!.jsonObject["progress"]!!.jsonPrimitive.int

    fun toTrack() = TrackSearch.create(TrackManager.KITSU).apply {
        media_id = libraryId
        title = canonicalTitle
        total_chapters = chapterCount ?: 0
        cover_url = original
        summary = synopsis
        tracking_url = KitsuApi.mangaUrl(media_id)
        publishing_status = this@KitsuLibManga.status
        publishing_type = type
        start_date = startDate
        started_reading_date = KitsuDateHelper.parse(startedAt)
        finished_reading_date = KitsuDateHelper.parse(finishedAt)
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
