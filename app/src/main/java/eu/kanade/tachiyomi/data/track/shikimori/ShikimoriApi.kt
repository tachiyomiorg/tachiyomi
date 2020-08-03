package eu.kanade.tachiyomi.data.track.shikimori

import androidx.core.net.toUri
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.obj
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import rx.Observable
import uy.kohesive.injekt.injectLazy

class ShikimoriApi(private val client: OkHttpClient, interceptor: ShikimoriInterceptor) {

    private val gson: Gson by injectLazy()
    private val jsonime = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    fun addLibManga(track: Track, user_id: String): Observable<Track> {
        val payload = jsonObject(
            "user_rate" to jsonObject(
                "user_id" to user_id,
                "target_id" to track.media_id,
                "target_type" to "Manga",
                "chapters" to track.last_chapter_read,
                "score" to track.score.toInt(),
                "status" to track.toShikimoriStatus()
            )
        )
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
                val response = JsonParser.parseString(responseBody).array
                response.map { jsonToSearch(it.obj) }
            }
    }

    private fun jsonToSearch(obj: JsonObject): TrackSearch {
        return TrackSearch.create(TrackManager.SHIKIMORI).apply {
            media_id = obj["id"].asInt
            title = obj["name"].asString
            total_chapters = obj["chapters"].asInt
            cover_url = baseUrl + obj["image"].obj["preview"].asString
            summary = ""
            tracking_url = baseUrl + obj["url"].asString
            publishing_status = obj["status"].asString
            publishing_type = obj["kind"].asString
            start_date = obj.get("aired_on").nullString.orEmpty()
        }
    }

    private fun jsonToTrack(obj: JsonObject, mangas: JsonObject): Track {
        return Track.create(TrackManager.SHIKIMORI).apply {
            title = mangas["name"].asString
            media_id = obj["id"].asInt
            total_chapters = mangas["chapters"].asInt
            last_chapter_read = obj["chapters"].asInt
            score = (obj["score"].asInt).toFloat()
            status = toTrackStatus(obj["status"].asString)
            tracking_url = baseUrl + mangas["url"].asString
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
                JsonParser.parseString(responseBody).obj
            }.flatMap { mangas ->
                authClient.newCall(request)
                    .asObservableSuccess()
                    .map { netResponse ->
                        val responseBody = netResponse.body?.string().orEmpty()
                        if (responseBody.isEmpty()) {
                            throw Exception("Null Response")
                        }
                        val response = JsonParser.parseString(responseBody).array
                        if (response.size() > 1) {
                            throw Exception("Too much mangas in response")
                        }
                        val entry = response.map {
                            jsonToTrack(it.obj, mangas)
                        }
                        entry.firstOrNull()
                    }
            }
    }

    fun getCurrentUser(): Int {
        val user = authClient.newCall(GET("$apiUrl/users/whoami")).execute().body?.string()
        return JsonParser.parseString(user).obj["id"].asInt
    }

    fun accessToken(code: String): Observable<OAuth> {
        return client.newCall(accessTokenRequest(code)).asObservableSuccess().map { netResponse ->
            val responseBody = netResponse.body?.string().orEmpty()
            if (responseBody.isEmpty()) {
                throw Exception("Null Response")
            }
            gson.fromJson(responseBody, OAuth::class.java)
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
        private const val apiUrl = "https://shikimori.one/api"
        private const val oauthUrl = "https://shikimori.one/oauth/token"
        private const val loginUrl = "https://shikimori.one/oauth/authorize"

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
