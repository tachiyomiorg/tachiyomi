package eu.kanade.tachiyomi.data.track.bangumi

import kotlinx.serialization.Serializable

@Serializable
data class Status(
    val id: Int? = 0,
    val name: String? = "",
    val type: String? = ""
)
