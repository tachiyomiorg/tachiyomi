package eu.kanade.tachiyomi.data.track.anilist

import android.net.Uri
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.nullInt
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.obj
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.asObservableSuccess
import java.util.Calendar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import rx.Observable

class AnilistApi(val client: OkHttpClient, interceptor: AnilistInterceptor) {

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
        val variables = jsonObject(
            "mangaId" to track.media_id,
            "progress" to track.last_chapter_read,
            "status" to track.toAnilistStatus()
        )
        val payload = jsonObject(
            "query" to query,
            "variables" to variables
        )
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
                val response = JsonParser.parseString(responseBody).obj
                track.library_id = response["data"]["SaveMediaListEntry"]["id"].asLong
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
        val variables = jsonObject(
            "listId" to track.library_id,
            "progress" to track.last_chapter_read,
            "status" to track.toAnilistStatus(),
            "score" to track.score.toInt()
        )
        val payload = jsonObject(
            "query" to query,
            "variables" to variables
        )
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
        val variables = jsonObject(
            "query" to search
        )
        val payload = jsonObject(
            "query" to query,
            "variables" to variables
        )
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
                val response = JsonParser.parseString(responseBody).obj
                val data = response["data"]!!.obj
                val page = data["Page"].obj
                val media = page["media"].array
                val entries = media.map { jsonToALManga(it.obj) }
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
        val variables = jsonObject(
            "id" to userid,
            "manga_id" to track.media_id
        )
        val payload = jsonObject(
            "query" to query,
            "variables" to variables
        )
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
                val response = JsonParser.parseString(responseBody).obj
                val data = response["data"]!!.obj
                val page = data["Page"].obj
                val media = page["mediaList"].array
                val entries = media.map { jsonToALUserManga(it.obj) }
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
        val payload = jsonObject(
            "query" to query
        )
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
                val response = JsonParser.parseString(responseBody).obj
                val data = response["data"]!!.obj
                val viewer = data["Viewer"].obj
                Pair(viewer["id"].asInt, viewer["mediaListOptions"]["scoreFormat"].asString)
            }
    }

    private fun jsonToALManga(struct: JsonObject): ALManga {
        val date = try {
            val date = Calendar.getInstance()
            date.set(
                struct["startDate"]["year"].nullInt ?: 0,
                (
                    struct["startDate"]["month"].nullInt
                        ?: 0
                    ) - 1,
                struct["startDate"]["day"].nullInt ?: 0
            )
            date.timeInMillis
        } catch (_: Exception) {
            0L
        }

        return ALManga(
            struct["id"].asInt, struct["title"]["romaji"].asString, struct["coverImage"]["large"].asString,
            struct["description"].nullString.orEmpty(), struct["type"].asString, struct["status"].nullString.orEmpty(),
            date, struct["chapters"].nullInt ?: 0
        )
    }

    private fun jsonToALUserManga(struct: JsonObject): ALUserManga {
        return ALUserManga(struct["id"].asLong, struct["status"].asString, struct["scoreRaw"].asInt, struct["progress"].asInt, jsonToALManga(struct["media"].obj))
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

        fun authUrl() = Uri.parse("${baseUrl}oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "token")
            .build()
    }
}
