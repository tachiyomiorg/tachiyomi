package eu.kanade.tachiyomi.data.track.shikimori

import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import rx.Observable
import uy.kohesive.injekt.injectLazy

class ShikimoriApi(private val client: OkHttpClient, interceptor: ShikimoriInterceptor) {

    private val json: Json by injectLazy()

    private val jsonime = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    fun addLibManga(track: Track, user_id: String): Observable<Track> {
        val payload = buildJsonObject {
            putJsonObject("user_rate") {
                put("user_id", user_id)
                put("target_id", track.media_id)
                put("target_type", "Manga")
                put("chapters", track.last_chapter_read)
                put("score", track.score.toInt())
                put("status", track.toShikimoriStatus())
            }
        }
        val body = payload.toString().toRequestBody(jsonime)
        val request = Request.Builder()
            .url("$apiUrl/v2/user_rates")
            .post(body)
            .build()
        return authClient.newCall(request)
            .asObservableSuccess()
            .map {
                track
            }
    }

    fun updateLibManga(track: Track, user_id: String): Observable<Track> = addLibManga(track, user_id)

    fun search(search: String): Observable<List<TrackSearch>> {
        val url = "$apiUrl/mangas".toUri().buildUpon()
            .appendQueryParameter("order", "popularity")
            .appendQueryParameter("search", search)
            .appendQueryParameter("limit", "20")
            .build()
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        return authClient.newCall(request)
            .asObservableSuccess()
            .map { netResponse ->
                val responseBody = netResponse.body?.string().orEmpty()
                if (responseBody.isEmpty()) {
                    throw Exception("Null Response")
                }
                val response = json.decodeFromString<JsonArray>(responseBody)
                response.map { jsonToSearch(it.jsonObject) }
            }
    }

    private fun jsonToSearch(obj: JsonObject): TrackSearch {
        return TrackSearch.create(TrackManager.SHIKIMORI).apply {
            media_id = obj["id"]!!.jsonPrimitive.int
            title = obj["name"]!!.jsonPrimitive.content
            total_chapters = obj["chapters"]!!.jsonPrimitive.int
            cover_url = baseUrl + obj["image"]!!.jsonObject["preview"]!!.jsonPrimitive.content
            summary = ""
            tracking_url = baseUrl + obj["url"]!!.jsonPrimitive.content
            publishing_status = obj["status"]!!.jsonPrimitive.content
            publishing_type = obj["kind"]!!.jsonPrimitive.content
            start_date = obj.get("aired_on")!!.jsonPrimitive.contentOrNull ?: ""
        }
    }

    private fun jsonToTrack(obj: JsonObject, mangas: JsonObject): Track {
        return Track.create(TrackManager.SHIKIMORI).apply {
            title = mangas["name"]!!.jsonPrimitive.content
            media_id = obj["id"]!!.jsonPrimitive.int
            total_chapters = mangas["chapters"]!!.jsonPrimitive.int
            last_chapter_read = obj["chapters"]!!.jsonPrimitive.int
            score = (obj["score"]!!.jsonPrimitive.int).toFloat()
            status = toTrackStatus(obj["status"]!!.jsonPrimitive.content)
            tracking_url = baseUrl + mangas["url"]!!.jsonPrimitive.content
        }
    }

    fun findLibManga(track: Track, user_id: String): Observable<Track?> {
        val url = "$apiUrl/v2/user_rates".toUri().buildUpon()
            .appendQueryParameter("user_id", user_id)
            .appendQueryParameter("target_id", track.media_id.toString())
            .appendQueryParameter("target_type", "Manga")
            .build()
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()

        val urlMangas = "$apiUrl/mangas".toUri().buildUpon()
            .appendPath(track.media_id.toString())
            .build()
        val requestMangas = Request.Builder()
            .url(urlMangas.toString())
            .get()
            .build()
        return authClient.newCall(requestMangas)
            .asObservableSuccess()
            .map { netResponse ->
                val responseBody = netResponse.body?.string().orEmpty()
                json.decodeFromString<JsonObject>(responseBody)
            }.flatMap { mangas ->
                authClient.newCall(request)
                    .asObservableSuccess()
                    .map { netResponse ->
                        val responseBody = netResponse.body?.string().orEmpty()
                        if (responseBody.isEmpty()) {
                            throw Exception("Null Response")
                        }
                        val response = json.decodeFromString<JsonArray>(responseBody)
                        if (response.size > 1) {
                            throw Exception("Too much mangas in response")
                        }
                        val entry = response.map {
                            jsonToTrack(it.jsonObject, mangas)
                        }
                        entry.firstOrNull()
                    }
            }
    }

    fun getCurrentUser(): Int {
        val user = authClient.newCall(GET("$apiUrl/users/whoami")).execute().body?.string()!!
        return json.decodeFromString<JsonObject>(user)["id"]!!.jsonPrimitive.int
    }

    fun accessToken(code: String): Observable<OAuth> {
        return client.newCall(accessTokenRequest(code)).asObservableSuccess().map { netResponse ->
            val responseBody = netResponse.body?.string().orEmpty()
            if (responseBody.isEmpty()) {
                throw Exception("Null Response")
            }
            json.decodeFromString<OAuth>(responseBody)
        }
    }

    private fun accessTokenRequest(code: String) = POST(
        oauthUrl,
        body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("redirect_uri", redirectUrl)
            .build()
    )

    companion object {
        private const val clientId = "1aaf4cf232372708e98b5abc813d795b539c5a916dbbfe9ac61bf02a360832cc"
        private const val clientSecret = "229942c742dd4cde803125d17d64501d91c0b12e14cb1e5120184d77d67024c0"

        private const val baseUrl = "https://shikimori.one"
        private const val apiUrl = "$baseUrl/api"
        private const val oauthUrl = "$baseUrl/oauth/token"
        private const val loginUrl = "$baseUrl/oauth/authorize"

        private const val redirectUrl = "tachiyomi://shikimori-auth"
        private const val baseMangaUrl = "$apiUrl/mangas"

        fun mangaUrl(remoteId: Int): String {
            return "$baseMangaUrl/$remoteId"
        }

        fun authUrl() =
            loginUrl.toUri().buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUrl)
                .appendQueryParameter("response_type", "code")
                .build()

        fun refreshTokenRequest(token: String) = POST(
            oauthUrl,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", token)
                .build()
        )
    }
}
