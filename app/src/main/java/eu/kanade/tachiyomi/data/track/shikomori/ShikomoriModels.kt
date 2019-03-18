package eu.kanade.tachiyomi.data.track.shikomori

import eu.kanade.tachiyomi.data.database.models.Track

fun Track.toShikomoriStatus() = when (status) {
    Shikomori.READING -> "watching"
    Shikomori.COMPLETED -> "completed"
    Shikomori.ON_HOLD -> "on_hold"
    Shikomori.DROPPED -> "dropped"
    Shikomori.PLANNING -> "planned"
    Shikomori.REPEATING -> "rewatching"
    else -> throw NotImplementedError("Unknown status")
}

fun toTrackStatus(status: String) = when (status) {
    "watching" -> Shikomori.READING
    "completed" -> Shikomori.COMPLETED
    "on_hold" -> Shikomori.ON_HOLD
    "dropped" -> Shikomori.DROPPED
    "planned" -> Shikomori.PLANNING
    "rewatching" -> Shikomori.REPEATING

    else -> throw Exception("Unknown status")
}
