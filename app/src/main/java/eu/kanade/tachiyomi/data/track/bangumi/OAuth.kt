package eu.kanade.tachiyomi.data.track.bangumi

import kotlinx.serialization.Serializable

@Serializable
data class OAuth(
    val access_token: String,
    val token_type: String,
    val created_at: Long = System.currentTimeMillis() / 1000,
    val expires_in: Long,
    val refresh_token: String?,
    val user_id: Long?
) {

    // Access token refresh before expired
    fun isExpired() = (System.currentTimeMillis() / 1000) > (created_at + expires_in - 3600)
}
