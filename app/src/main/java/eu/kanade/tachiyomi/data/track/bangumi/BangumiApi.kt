package eu.kanade.tachiyomi.data.track.bangumi

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder

class BangumiApi(private val client: OkHttpClient, interceptor: BangumiInterceptor) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    fun addLibManga(track: Track): Observable<Track> {
        val body = FormBody.Builder()
            .add("rating", track.score.toInt().toString())
            .add("status", track.toBangumiStatus())
            .build()
        val request = Request.Builder()
            .url("$apiUrl/collection/${track.media_id}/update")
            .post(body)
            .build()
        return authClient.newCall(request)
            .asObservableSuccess()
            .map {
                track
            }
    }

    fun updateLibManga(track: Track): Observable<Track> {
        // chapter update
        val body = FormBody.Builder()
            .add("watched_eps", track.last_chapter_read.toString())
            .build()
        val request = Request.Builder()
            .url("$apiUrl/subject/${track.media_id}/update/watched_eps")
            .post(body)
            .build()

        // read status update
        val sbody = FormBody.Builder()
            .add("status", track.toBangumiStatus())
            .build()
        val srequest = Request.Builder()
            .url("$apiUrl/collection/${track.media_id}/update")
            .post(sbody)
            .build()
        return authClient.newCall(srequest)
            .asObservableSuccess()
            .map {
                track
            }.flatMap {
                authClient.newCall(request)
                    .asObservableSuccess()
                    .map {
                        track
                    }
            }
    }

    fun search(search: String): Observable<List<TrackSearch>> {
        val url = "$apiUrl/search/subject/${URLEncoder.encode(search, Charsets.UTF_8.name())}"
            .toUri()
            .buildUpon()
            .appendQueryParameter("max_results", "20")
            .build()
        val request = Request.Builder()
            .url(url.toString())
            .get()
            .build()
        return authClient.newCall(request)
            .asObservableSuccess()
            .map { netResponse ->
                var responseBody = netResponse.body?.string().orEmpty()
                if (responseBody.isEmpty()) {
                    throw Exception("Null Response")
                }
                if (responseBody.contains("\"code\":404")) {
                    responseBody = "{\"results\":0,\"list\":[]}"
                }
                val response = json.decodeFromString<JsonObject>(responseBody)["list"]?.jsonArray
                response?.filter { it.jsonObject["type"]?.jsonPrimitive?.int == 1 }?.map { jsonToSearch(it.jsonObject) }
            }
    }

    private fun jsonToSearch(obj: JsonObject): TrackSearch {
        return TrackSearch.create(TrackManager.BANGUMI).apply {
            media_id = obj["id"]!!.jsonPrimitive.int
            title = obj["name_cn"]!!.jsonPrimitive.content
            cover_url = obj["images"]!!.jsonObject["common"]!!.jsonPrimitive.content
            summary = obj["name"]!!.jsonPrimitive.content
            tracking_url = obj["url"]!!.jsonPrimitive.content
        }
    }

    private fun jsonToTrack(mangas: JsonObject): Track {
        return Track.create(TrackManager.BANGUMI).apply {
            title = mangas["name"]!!.jsonPrimitive.content
            media_id = mangas["id"]!!.jsonPrimitive.int
            score = try {
                mangas["rating"]!!.jsonObject["score"]!!.jsonPrimitive.float
            } catch (_: Exception) {
                0f
            }
            status = Bangumi.DEFAULT_STATUS
            tracking_url = mangas["url"]!!.jsonPrimitive.content
        }
    }

    fun findLibManga(track: Track): Observable<Track?> {
        val urlMangas = "$apiUrl/subject/${track.media_id}"
        val requestMangas = Request.Builder()
            .url(urlMangas)
            .get()
            .build()

        return authClient.newCall(requestMangas)
            .asObservableSuccess()
            .map { netResponse ->
                // get comic info
                val responseBody = netResponse.body?.string().orEmpty()
                jsonToTrack(json.decodeFromString(responseBody))
            }
    }

    fun statusLibManga(track: Track): Observable<Track?> {
        val urlUserRead = "$apiUrl/collection/${track.media_id}"
        val requestUserRead = Request.Builder()
            .url(urlUserRead)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .get()
            .build()

        // todo get user readed chapter here
        return authClient.newCall(requestUserRead)
            .asObservableSuccess()
            .map { netResponse ->
                val resp = netResponse.body?.string()
                val coll = json.decodeFromString<Collection>(resp!!)
                track.status = coll.status?.id!!
                track.last_chapter_read = coll.ep_status!!
                track
            }
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
        private const val clientId = "bgm10555cda0762e80ca"
        private const val clientSecret = "8fff394a8627b4c388cbf349ec865775"

        private const val apiUrl = "https://api.bgm.tv"
        private const val oauthUrl = "https://bgm.tv/oauth/access_token"
        private const val loginUrl = "https://bgm.tv/oauth/authorize"

        private const val redirectUrl = "tachiyomi://bangumi-auth"
        private const val baseMangaUrl = "$apiUrl/mangas"

        fun mangaUrl(remoteId: Int): String {
            return "$baseMangaUrl/$remoteId"
        }

        fun authUrl(): Uri =
            loginUrl.toUri().buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", redirectUrl)
                .build()

        fun refreshTokenRequest(token: String) = POST(
            oauthUrl,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", token)
                .add("redirect_uri", redirectUrl)
                .build()
        )
    }
}
