package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.database.models.Track

fun Track.toShikimoriStatus() = when (status) {
    Shikimori.READING -> "watching"
    Shikimori.COMPLETED -> "completed"
    Shikimori.ON_HOLD -> "on_hold"
    Shikimori.DROPPED -> "dropped"
    Shikimori.PLANNING -> "planned"
    Shikimori.REPEATING -> "rewatching"
    else -> throw NotImplementedError("Unknown status")
}

fun toTrackStatus(status: String) = when (status) {
    "watching" -> Shikimori.READING
    "completed" -> Shikimori.COMPLETED
    "on_hold" -> Shikimori.ON_HOLD
    "dropped" -> Shikimori.DROPPED
    "planned" -> Shikimori.PLANNING
    "rewatching" -> Shikimori.REPEATING

    else -> throw Exception("Unknown status")
}
