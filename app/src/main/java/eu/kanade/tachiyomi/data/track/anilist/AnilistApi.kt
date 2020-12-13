package eu.kanade.tachiyomi.data.track.anilist

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.asObservableSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.util.Calendar

class AnilistApi(val client: OkHttpClient, interceptor: AnilistInterceptor) {

    private val json: Json by injectLazy()

    private val jsonMime = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    fun addLibManga(track: Track): Observable<Track> {
        val query =
            """
            |mutation AddManga(${'$'}mangaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus) {
                |SaveMediaListEntry (mediaId: ${'$'}mangaId, progress: ${'$'}progress, status: ${'$'}status) { 
                |   id 
                |   status 
                |} 
            |}
            |""".trimMargin()
        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") {
                put("mangaId", track.media_id)
                put("progress", track.last_chapter_read)
                put("status", track.toAnilistStatus())
            }
        }
        val body = payload.toString().toRequestBody(jsonMime)
        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .build()
        return authClient.newCall(request)
            .asObservableSuccess()
            .map { netResponse ->
                val responseBody = netResponse.body?.string().orEmpty()
                netResponse.close()
                if (responseBody.isEmpty()) {
                    throw Exception("Null Response")
                }
                val response = json.decodeFromString<JsonObject>(responseBody)
                track.library_id = response["data"]!!.jsonObject["SaveMediaListEntry"]!!.jsonObject["id"]!!.jsonPrimitive.long
                track
            }
    }

    fun updateLibManga(track: Track): Observable<Track> {
        val query =
            """
            |mutation UpdateManga(${'$'}listId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}score: Int) {
                |SaveMediaListEntry (id: ${'$'}listId, progress: ${'$'}progress, status: ${'$'}status, scoreRaw: ${'$'}score) {
                    |id
                    |status
                    |progress
                |}
            |}
            |""".trimMargin()
        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") {
                put("listId", track.library_id)
                put("progress", track.last_chapter_read)
                put("status", track.toAnilistStatus())
                put("score", track.score.toInt())
            }
        }
        val body = payload.toString().toRequestBody(jsonMime)
        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .build()
        return authClient.newCall(request)
            .asObservableSuccess()
            .map {
                track
            }
    }

    fun search(search: String): Observable<List<TrackSearch>> {
        val query =
            """
            |query Search(${'$'}query: String) {
                |Page (perPage: 50) {
                    |media(search: ${'$'}query, type: MANGA, format_not_in: [NOVEL]) {
                        |id
                        |title {
                            |romaji
                        |}
                        |coverImage {
                            |large
                        |}
                        |type
                        |status
                        |chapters
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                    |}
                |}
            |}
            |""".trimMargin()
        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") {
                put("query", search)
            }
        }
        val body = payload.toString().toRequestBody(jsonMime)
        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .build()
        return authClient.newCall(request)
            .asObservableSuccess()
            .map { netResponse ->
                val responseBody = netResponse.body?.string().orEmpty()
                if (responseBody.isEmpty()) {
                    throw Exception("Null Response")
                }
                val response = json.decodeFromString<JsonObject>(responseBody)
                val data = response["data"]!!.jsonObject
                val page = data["Page"]!!.jsonObject
                val media = page["media"]!!.jsonArray
                val entries = media.map { jsonToALManga(it.jsonObject) }
                entries.map { it.toTrack() }
            }
    }

    fun findLibManga(track: Track, userid: Int): Observable<Track?> {
        val query =
            """
            |query (${'$'}id: Int!, ${'$'}manga_id: Int!) {
                |Page {
                    |mediaList(userId: ${'$'}id, type: MANGA, mediaId: ${'$'}manga_id) {
                        |id
                        |status
                        |scoreRaw: score(format: POINT_100)
                        |progress
                        |media {
                            |id
                            |title {
                                |romaji
                            |}
                            |coverImage {
                                |large
                            |}
                            |type
                            |status
                            |chapters
                            |description
                            |startDate {
                                |year
                                |month
                                |day
                            |}
                        |}
                    |}
                |}
            |}
            |""".trimMargin()
        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") {
                put("id", userid)
                put("manga_id", track.media_id)
            }
        }
        val body = payload.toString().toRequestBody(jsonMime)
        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .build()
        return authClient.newCall(request)
            .asObservableSuccess()
            .map { netResponse ->
                val responseBody = netResponse.body?.string().orEmpty()
                if (responseBody.isEmpty()) {
                    throw Exception("Null Response")
                }
                val response = json.decodeFromString<JsonObject>(responseBody)
                val data = response["data"]!!.jsonObject
                val page = data["Page"]!!.jsonObject
                val media = page["mediaList"]!!.jsonArray
                val entries = media.map { jsonToALUserManga(it.jsonObject) }
                entries.firstOrNull()?.toTrack()
            }
    }

    fun getLibManga(track: Track, userid: Int): Observable<Track> {
        return findLibManga(track, userid)
            .map { it ?: throw Exception("Could not find manga") }
    }

    fun createOAuth(token: String): OAuth {
        return OAuth(token, "Bearer", System.currentTimeMillis() + 31536000000, 31536000000)
    }

    fun getCurrentUser(): Observable<Pair<Int, String>> {
        val query =
            """
            |query User {
                |Viewer {
                    |id
                    |mediaListOptions {
                        |scoreFormat
                    |}
                |}
            |}
            |""".trimMargin()
        val payload = buildJsonObject {
            put("query", query)
        }
        val body = payload.toString().toRequestBody(jsonMime)
        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .build()
        return authClient.newCall(request)
            .asObservableSuccess()
            .map { netResponse ->
                val responseBody = netResponse.body?.string().orEmpty()
                if (responseBody.isEmpty()) {
                    throw Exception("Null Response")
                }
                val response = json.decodeFromString<JsonObject>(responseBody)
                val data = response["data"]!!.jsonObject
                val viewer = data["Viewer"]!!.jsonObject
                Pair(viewer["id"]!!.jsonPrimitive.int, viewer["mediaListOptions"]!!.jsonObject["scoreFormat"]!!.jsonPrimitive.content)
            }
    }

    private fun jsonToALManga(struct: JsonObject): ALManga {
        val date = try {
            val date = Calendar.getInstance()
            date.set(
                struct["startDate"]!!.jsonObject["year"]!!.jsonPrimitive.intOrNull ?: 0,
                (
                    struct["startDate"]!!.jsonObject["month"]!!.jsonPrimitive.intOrNull
                        ?: 0
                    ) - 1,
                struct["startDate"]!!.jsonObject["day"]!!.jsonPrimitive.intOrNull ?: 0
            )
            date.timeInMillis
        } catch (_: Exception) {
            0L
        }

        return ALManga(
            struct["id"]!!.jsonPrimitive.int,
            struct["title"]!!.jsonObject["romaji"]!!.jsonPrimitive.content,
            struct["coverImage"]!!.jsonObject["large"]!!.jsonPrimitive.content,
            struct["description"]!!.jsonPrimitive.contentOrNull,
            struct["type"]!!.jsonPrimitive.content,
            struct["status"]!!.jsonPrimitive.contentOrNull ?: "",
            date,
            struct["chapters"]!!.jsonPrimitive.intOrNull ?: 0
        )
    }

    private fun jsonToALUserManga(struct: JsonObject): ALUserManga {
        return ALUserManga(
            struct["id"]!!.jsonPrimitive.long,
            struct["status"]!!.jsonPrimitive.content,
            struct["scoreRaw"]!!.jsonPrimitive.int,
            struct["progress"]!!.jsonPrimitive.int,
            jsonToALManga(struct["media"]!!.jsonObject)
        )
    }

    companion object {
        private const val clientId = "385"
        private const val clientUrl = "tachiyomi://anilist-auth"
        private const val apiUrl = "https://graphql.anilist.co/"
        private const val baseUrl = "https://anilist.co/api/v2/"
        private const val baseMangaUrl = "https://anilist.co/manga/"

        fun mangaUrl(mediaId: Int): String {
            return baseMangaUrl + mediaId
        }

        fun authUrl(): Uri = "${baseUrl}oauth/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "token")
            .build()
    }
}
