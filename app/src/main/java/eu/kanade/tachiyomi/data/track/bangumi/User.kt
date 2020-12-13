package eu.kanade.tachiyomi.data.track.bangumi

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val avatar: Avatar? = Avatar(),
    val id: Int? = 0,
    val nickname: String? = "",
    val sign: String? = "",
    val url: String? = "",
    val usergroup: Int? = 0,
    val username: String? = ""
)
