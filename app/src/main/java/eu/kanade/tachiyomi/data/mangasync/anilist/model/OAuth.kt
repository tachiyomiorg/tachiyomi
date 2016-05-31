package eu.kanade.tachiyomi.data.mangasync.anilist.model

data class OAuth(
        val access_token: String,
        val token_type: String,
        val expires: Long,
        val expires_in: Long,
        val refresh_token: String?) {

    fun isExpired() = System.currentTimeMillis() > expires
}