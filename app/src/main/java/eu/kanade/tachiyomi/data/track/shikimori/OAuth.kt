package eu.kanade.tachiyomi.data.track.shikimori

data class OAuth(
        val access_token: String,
        val token_type: String,
        val created_at: Long,
        val expires_in: Long,
        val refresh_token: String?) {

    // Access token lives 1 day
    fun isExpired() = (System.currentTimeMillis() / 1000) > (created_at + expires_in - 3600)
}

