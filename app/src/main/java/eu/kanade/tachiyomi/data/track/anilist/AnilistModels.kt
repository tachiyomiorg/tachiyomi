package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.*

data class ALManga(
        val media_id: Int,
        val title_romaji: String,
        val image_url_lge: String,
        val description: String?,
        val type: String,
        val publishing_status: String,
        val start_date_fuzzy: String,
        val total_chapters: Int) {

    fun toTrack() = TrackSearch.create(TrackManager.ANILIST).apply {
        media_id = this@ALManga.media_id
        title = title_romaji
        total_chapters = this@ALManga.total_chapters
        cover_url = image_url_lge
        summary = description ?: ""
        tracking_url = AnilistApi.mangaUrl(media_id)
        publishing_status = this@ALManga.publishing_status
        publishing_type = type
        if (!start_date_fuzzy.isNullOrBlank()) {
            start_date = try {
                val inputDf = SimpleDateFormat("yyyyMMdd", Locale.US)
                val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = inputDf.parse(BuildConfig.BUILD_TIME)
                outputDf.format(date)
            } catch (e: Exception) {
                start_date_fuzzy.orEmpty()
            }
        }
    }
}

data class ALUserManga(
        val library_id: Long,
        val list_status: String,
        val score_raw: Int,
        val chapters_read: Int,
        val manga: ALManga) {

    fun toTrack() = Track.create(TrackManager.ANILIST).apply {
        media_id = manga.media_id
        status = toTrackStatus()
        score = score_raw.toFloat()
        last_chapter_read = chapters_read
        library_id = this@ALUserManga.library_id
    }

    fun toTrackStatus() = when (list_status) {
        "CURRENT" -> Anilist.READING
        "COMPLETED" -> Anilist.COMPLETED
        "PAUSED" -> Anilist.ON_HOLD
        "DROPPED" -> Anilist.DROPPED
        "PLANNING" -> Anilist.PLANNING
        else -> throw NotImplementedError("Unknown status")
    }
}

fun Track.toAnilistStatus() = when (status) {
    Anilist.READING -> "CURRENT"
    Anilist.COMPLETED -> "COMPLETED"
    Anilist.ON_HOLD -> "PAUSED"
    Anilist.DROPPED -> "DROPPED"
    Anilist.PLANNING -> "PLANNING"
    Anilist.REPEATING -> "REPEATING"
    else -> throw NotImplementedError("Unknown status")
}

private val preferences: PreferencesHelper by injectLazy()

fun Track.toAnilistScore(): String = when (preferences.anilistScoreType().getOrDefault()) {
// 10 point
    "POINT_10" -> (score.toInt() / 10).toString()
// 100 point
    "POINT_100" -> score.toInt().toString()
// 5 stars
    "POINT_5" -> when {
        score == 0f -> "0"
        score < 30 -> "1"
        score < 50 -> "2"
        score < 70 -> "3"
        score < 90 -> "4"
        else -> "5"
    }
// Smiley
    "POINT_3" -> when {
        score == 0f -> "0"
        score <= 30 -> ":("
        score <= 60 -> ":|"
        else -> ":)"
    }
// 10 point decimal
    "POINT_10_DECIMAL" -> (score / 10).toString()
    else -> throw Exception("Unknown score type")
}
