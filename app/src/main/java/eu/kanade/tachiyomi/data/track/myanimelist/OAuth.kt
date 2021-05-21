package eu.kanade.tachiyomi.data.track.myanimelist

import kotlinx.serialization.Serializable

@Serializable
data class OAuth(
    val refresh_token: String,
    val access_token: String,
    val token_type: String,
    val created_at: Long = System.currentTimeMillis(),
    val expires_in: Long
) {

    fun isExpired() = System.currentTimeMillis() > created_at + (expires_in * 1000)
}
