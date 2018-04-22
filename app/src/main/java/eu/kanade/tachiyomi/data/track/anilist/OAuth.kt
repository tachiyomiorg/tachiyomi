package eu.kanade.tachiyomi.data.track.anilist

data class OAuth(
        val access_token: String,
        val token_type: String,
        val expires: Long,
        val expires_in: Long) {

    fun isExpired() = System.currentTimeMillis() > expires
}