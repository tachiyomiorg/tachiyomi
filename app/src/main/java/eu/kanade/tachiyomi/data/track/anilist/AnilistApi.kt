package eu.kanade.tachiyomi.data.track.anilist

import android.net.Uri
import androidx.core.net.toUri
import com.afollestad.date.dayOfMonth
import com.afollestad.date.month
import com.afollestad.date.year
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.interceptor.RateLimitInterceptor
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.serialization.json.JsonNull
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
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Calendar
import java.util.concurrent.TimeUnit.MINUTES

class AnilistApi(val client: OkHttpClient, interceptor: AnilistInterceptor) {

    private val authClient = client.newBuilder()
        .addInterceptor(interceptor)
        .addInterceptor(RateLimitInterceptor(85, 1, MINUTES))
        .build()

    suspend fun addLibManga(track: Track): Track {
        return withIOContext {
            val query = """
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
            authClient.newCall(
                POST(
                    apiUrl,
                    body = payload.toString().toRequestBody(jsonMime)
                )
            )
                .await()
                .parseAs<JsonObject>()
                .let {
                    track.library_id =
                        it["data"]!!.jsonObject["SaveMediaListEntry"]!!.jsonObject["id"]!!.jsonPrimitive.long
                    track
                }
        }
    }

    suspend fun updateLibManga(track: Track): Track {
        return withIOContext {
            val query = """
            |mutation UpdateManga(
                |${'$'}listId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus,
                |${'$'}score: Int, ${'$'}startedAt: FuzzyDateInput, ${'$'}completedAt: FuzzyDateInput
            |) {
                |SaveMediaListEntry(
                    |id: ${'$'}listId, progress: ${'$'}progress, status: ${'$'}status,
                    |scoreRaw: ${'$'}score, startedAt: ${'$'}startedAt, completedAt: ${'$'}completedAt
                |) {
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
                    put("startedAt", createDate(track.started_reading_date))
                    put("completedAt", createDate(track.finished_reading_date))
                }
            }
            authClient.newCall(POST(apiUrl, body = payload.toString().toRequestBody(jsonMime)))
                .await()
            track
        }
    }

    suspend fun search(search: String): List<TrackSearch> {
        return withIOContext {
            val query = """
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
            authClient.newCall(
                POST(
                    apiUrl,
                    body = payload.toString().toRequestBody(jsonMime)
                )
            )
                .await()
                .parseAs<JsonObject>()
                .let { response ->
                    val data = response["data"]!!.jsonObject
                    val page = data["Page"]!!.jsonObject
                    val media = page["media"]!!.jsonArray
                    val entries = media.map { jsonToALManga(it.jsonObject) }
                    entries.map { it.toTrack() }
                }
        }
    }

    suspend fun findLibManga(track: Track, userid: Int): Track? {
        return withIOContext {
            val query = """
            |query (${'$'}id: Int!, ${'$'}manga_id: Int!) {
                |Page {
                    |mediaList(userId: ${'$'}id, type: MANGA, mediaId: ${'$'}manga_id) {
                        |id
                        |status
                        |scoreRaw: score(format: POINT_100)
                        |progress
                        |startedAt {
                            |year
                            |month
                            |day
                        |}
                        |completedAt {
                            |year
                            |month
                            |day
                        |}
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
            authClient.newCall(
                POST(
                    apiUrl,
                    body = payload.toString().toRequestBody(jsonMime)
                )
            )
                .await()
                .parseAs<JsonObject>()
                .let { response ->
                    val data = response["data"]!!.jsonObject
                    val page = data["Page"]!!.jsonObject
                    val media = page["mediaList"]!!.jsonArray
                    val entries = media.map { jsonToALUserManga(it.jsonObject) }
                    entries.firstOrNull()?.toTrack()
                }
        }
    }

    suspend fun getLibManga(track: Track, userid: Int): Track {
        return findLibManga(track, userid) ?: throw Exception("Could not find manga")
    }

    fun createOAuth(token: String): OAuth {
        return OAuth(token, "Bearer", System.currentTimeMillis() + 31536000000, 31536000000)
    }

    suspend fun getCurrentUser(): Pair<Int, String> {
        return withIOContext {
            val query = """
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
            authClient.newCall(
                POST(
                    apiUrl,
                    body = payload.toString().toRequestBody(jsonMime)
                )
            )
                .await()
                .parseAs<JsonObject>()
                .let {
                    val data = it["data"]!!.jsonObject
                    val viewer = data["Viewer"]!!.jsonObject
                    Pair(
                        viewer["id"]!!.jsonPrimitive.int,
                        viewer["mediaListOptions"]!!.jsonObject["scoreFormat"]!!.jsonPrimitive.content
                    )
                }
        }
    }

    private fun jsonToALManga(struct: JsonObject): ALManga {
        return ALManga(
            struct["id"]!!.jsonPrimitive.int,
            struct["title"]!!.jsonObject["romaji"]!!.jsonPrimitive.content,
            struct["coverImage"]!!.jsonObject["large"]!!.jsonPrimitive.content,
            struct["description"]!!.jsonPrimitive.contentOrNull,
            struct["type"]!!.jsonPrimitive.content,
            struct["status"]!!.jsonPrimitive.contentOrNull ?: "",
            parseDate(struct, "startDate"),
            struct["chapters"]!!.jsonPrimitive.intOrNull ?: 0
        )
    }

    private fun jsonToALUserManga(struct: JsonObject): ALUserManga {
        return ALUserManga(
            struct["id"]!!.jsonPrimitive.long,
            struct["status"]!!.jsonPrimitive.content,
            struct["scoreRaw"]!!.jsonPrimitive.int,
            struct["progress"]!!.jsonPrimitive.int,
            parseDate(struct, "startedAt"),
            parseDate(struct, "completedAt"),
            jsonToALManga(struct["media"]!!.jsonObject)
        )
    }

    private fun parseDate(struct: JsonObject, dateKey: String): Long {
        return try {
            val date = Calendar.getInstance()
            date.set(
                struct[dateKey]!!.jsonObject["year"]!!.jsonPrimitive.int,
                struct[dateKey]!!.jsonObject["month"]!!.jsonPrimitive.int - 1,
                struct[dateKey]!!.jsonObject["day"]!!.jsonPrimitive.int
            )
            date.timeInMillis
        } catch (_: Exception) {
            0L
        }
    }

    private fun createDate(dateValue: Long): JsonObject {
        if (dateValue == 0L) {
            return buildJsonObject {
                put("year", JsonNull)
                put("month", JsonNull)
                put("day", JsonNull)
            }
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateValue
        return buildJsonObject {
            put("year", calendar.year)
            put("month", calendar.month + 1)
            put("day", calendar.dayOfMonth)
        }
    }

    companion object {
        private const val clientId = "385"
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
