package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.data.database.models.Track

fun Track.toBangumiStatus() = when (status) {
    Bangumi.READING -> "watching"
    Bangumi.COMPLETED -> "completed"
    Bangumi.ON_HOLD -> "on_hold"
    Bangumi.DROPPED -> "dropped"
    Bangumi.PLANNING -> "planned"
    Bangumi.REPEATING -> "rewatching"
    else -> throw NotImplementedError("Unknown status")
}

fun toTrackStatus(status: String) = when (status) {
    "watching" -> Bangumi.READING
    "completed" -> Bangumi.COMPLETED
    "on_hold" -> Bangumi.ON_HOLD
    "dropped" -> Bangumi.DROPPED
    "planned" -> Bangumi.PLANNING
    "rewatching" -> Bangumi.REPEATING

    else -> throw Exception("Unknown status")
}
