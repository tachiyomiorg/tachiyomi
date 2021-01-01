package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.database.models.Track

fun Track.toMyAnimeListStatus() = when (status) {
    MyAnimeList.READING -> "reading"
    MyAnimeList.COMPLETED -> "completed"
    MyAnimeList.ON_HOLD -> "on_hold"
    MyAnimeList.DROPPED -> "dropped"
    MyAnimeList.PLAN_TO_READ -> "plan_to_read"
    MyAnimeList.REREADING -> "reading"
    else -> null
}

fun getStatus(status: String) = when (status) {
    "reading" -> MyAnimeList.READING
    "completed" -> MyAnimeList.COMPLETED
    "on_hold" -> MyAnimeList.ON_HOLD
    "dropped" -> MyAnimeList.DROPPED
    "plan_to_read" -> MyAnimeList.PLAN_TO_READ
    else -> MyAnimeList.READING
}
