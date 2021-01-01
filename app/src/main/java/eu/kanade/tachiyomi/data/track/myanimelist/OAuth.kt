package eu.kanade.tachiyomi.data.track.myanimelist

import kotlinx.serialization.Serializable

@Serializable
data class OAuth(
    val refresh_token: String,
    val access_token: String,
    val token_type: String,
    val expires_in: Long
) {

    fun isExpired() = System.currentTimeMillis() > expires_in
}
