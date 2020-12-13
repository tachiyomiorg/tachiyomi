package eu.kanade.tachiyomi.data.track.bangumi

import kotlinx.serialization.Serializable

@Serializable
data class Avatar(
    val large: String? = "",
    val medium: String? = "",
    val small: String? = ""
)
