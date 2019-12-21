package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.data.database.models.Track

fun Track.toBangumiStatus() = when (status) {
  Bangumi.READING -> "do"
  Bangumi.COMPLETED -> "collect"
  Bangumi.ON_HOLD -> "on_hold"
  Bangumi.DROPPED -> "dropped"
  Bangumi.PLANNING -> "wish"
  else -> throw NotImplementedError("Unknown status")
}

fun toTrackStatus(status: String) = when (status) {
  "do" -> Bangumi.READING
  "collect" -> Bangumi.COMPLETED
  "on_hold" -> Bangumi.ON_HOLD
  "dropped" -> Bangumi.DROPPED
  "wish" -> Bangumi.PLANNING

  else -> throw Exception("Unknown status")
}
