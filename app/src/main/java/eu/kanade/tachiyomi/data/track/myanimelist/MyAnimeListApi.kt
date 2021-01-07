package eu.kanade.tachiyomi.data.track.myanimelist

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.PkceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.text.SimpleDateFormat
import java.util.Locale

class MyAnimeListApi(private val client: OkHttpClient, interceptor: MyAnimeListInterceptor) {

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun getAccessToken(authCode: String): OAuth {
        return withContext(Dispatchers.IO) {
            val formBody: RequestBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("code", authCode)
                .add("code_verifier", codeVerifier)
                .add("grant_type", "authorization_code")
                .build()
            client.newCall(POST("$baseOAuthUrl/token", body = formBody))
                .await()
                .parseAs()
        }
    }

    suspend fun getCurrentUser(): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseApiUrl/users/@me")
                .get()
                .build()
            authClient.newCall(request)
                .await()
                .parseAs<JsonObject>()
                .let { it["name"]!!.jsonPrimitive.content }
        }
    }

    suspend fun search(query: String): List<TrackSearch> {
        return withContext(Dispatchers.IO) {
            val url = "$baseApiUrl/manga".toUri().buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter("nsfw", "true")
                .build()
            authClient.newCall(GET(url.toString()))
                .await()
                .parseAs<JsonObject>()
                .let {
                    it["data"]!!.jsonArray
                        .map { data -> data.jsonObject["node"]!!.jsonObject }
                        .map { node ->
                            val id = node["id"]!!.jsonPrimitive.int
                            async { getMangaDetails(id) }
                        }
                        .awaitAll()
                        .filter { trackSearch -> trackSearch.publishing_type != "novel" }
                }
        }
    }

    suspend fun getMangaDetails(id: Int): TrackSearch {
        return withContext(Dispatchers.IO) {
            val url = "$baseApiUrl/manga".toUri().buildUpon()
                .appendPath(id.toString())
                .appendQueryParameter("fields", "id,title,synopsis,num_chapters,main_picture,status,media_type,start_date")
                .build()
            authClient.newCall(GET(url.toString()))
                .await()
                .parseAs<JsonObject>()
                .let {
                    val obj = it.jsonObject
                    TrackSearch.create(TrackManager.MYANIMELIST).apply {
                        media_id = obj["id"]!!.jsonPrimitive.int
                        title = obj["title"]!!.jsonPrimitive.content
                        summary = obj["synopsis"]?.jsonPrimitive?.content ?: ""
                        total_chapters = obj["num_chapters"]!!.jsonPrimitive.int
                        cover_url = obj["main_picture"]?.jsonObject?.get("large")?.jsonPrimitive?.content ?: ""
                        tracking_url = "https://myanimelist.net/manga/$media_id"
                        publishing_status = obj["status"]!!.jsonPrimitive.content.replace("_", " ")
                        publishing_type = obj["media_type"]!!.jsonPrimitive.content.replace("_", " ")
                        start_date = try {
                            val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            outputDf.format(obj["start_date"]!!)
                        } catch (e: Exception) {
                            ""
                        }
                    }
                }
        }
    }

    suspend fun getListItem(track: Track): Track {
        return withContext(Dispatchers.IO) {
            val formBody: RequestBody = FormBody.Builder()
                .add("status", track.toMyAnimeListStatus() ?: "reading")
                .build()
            val request = Request.Builder()
                .url(mangaUrl(track.media_id).toString())
                .put(formBody)
                .build()
            authClient.newCall(request)
                .await()
                .parseAs<JsonObject>()
                .let { parseMangaItem(it, track) }
        }
    }

    suspend fun addItemToList(track: Track): Track {
        return withContext(Dispatchers.IO) {
            val formBody: RequestBody = FormBody.Builder()
                .add("status", "reading")
                .add("score", "0")
                .build()
            val request = Request.Builder()
                .url(mangaUrl(track.media_id).toString())
                .put(formBody)
                .build()
            authClient.newCall(request)
                .await()
                .parseAs<JsonObject>()
                .let { parseMangaItem(it, track) }
        }
    }

    suspend fun updateItem(track: Track): Track {
        return withContext(Dispatchers.IO) {
            val formBody: RequestBody = FormBody.Builder()
                .add("status", track.toMyAnimeListStatus() ?: "reading")
                .add("is_rereading", (track.status == MyAnimeList.REREADING).toString())
                .add("score", track.score.toString())
                .add("num_chapters_read", track.last_chapter_read.toString())
                .build()
            val request = Request.Builder()
                .url(mangaUrl(track.media_id).toString())
                .put(formBody)
                .build()
            authClient.newCall(request)
                .await()
                .parseAs<JsonObject>()
                .let { parseMangaItem(it, track) }
        }
    }

    suspend fun findListItem(track: Track, offset: Int = 0): Track? {
        val json = getListPage(offset)
        val obj = json.jsonObject
        val trackedManga = obj["data"]!!.jsonArray.find { data ->
            data.jsonObject["node"]!!.jsonObject["id"]!!.jsonPrimitive.int == track.media_id
        }

        return when {
            // Found the item in the list
            trackedManga != null -> {
                parseMangaItem(trackedManga.jsonObject["list_status"]!!.jsonObject, track)
            }
            // Check next page if there's more
            !obj["paging"]!!.jsonObject["next"]?.jsonPrimitive?.contentOrNull.isNullOrBlank() -> {
                findListItem(track, offset + listPaginationAmount)
            }
            // No more pages to check, item wasn't found
            else -> {
                null
            }
        }
    }

    suspend fun findListItems(query: String, offset: Int = 0): List<TrackSearch> {
        return withContext(Dispatchers.IO) {
            val json = getListPage(offset)
            val obj = json.jsonObject

            val matches = obj["data"]!!.jsonArray
                .filter {
                    it.jsonObject["node"]!!.jsonObject["title"]!!.jsonPrimitive.content.contains(
                        query,
                        ignoreCase = true
                    )
                }
                .map {
                    val id = it.jsonObject["node"]!!.jsonObject["id"]!!.jsonPrimitive.int
                    async { getMangaDetails(id) }
                }
                .awaitAll()

            // Check next page if there's more
            if (!obj["paging"]!!.jsonObject["next"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) {
                matches + findListItems(query, offset + listPaginationAmount)
            } else {
                matches
            }
        }
    }

    private suspend fun getListPage(offset: Int): JsonObject {
        return withContext(Dispatchers.IO) {
            val urlBuilder = "$baseApiUrl/users/@me/mangalist".toUri().buildUpon()
                .appendQueryParameter("fields", "list_status")
                .appendQueryParameter("limit", listPaginationAmount.toString())
            if (offset > 0) {
                urlBuilder.appendQueryParameter("offset", offset.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build().toString())
                .get()
                .build()
            authClient.newCall(request)
                .await()
                .parseAs()
        }
    }

    private fun parseMangaItem(response: JsonObject, track: Track): Track {
        val obj = response.jsonObject
        return track.apply {
            val isRereading = obj["is_rereading"]!!.jsonPrimitive.boolean
            status = if (isRereading) MyAnimeList.REREADING else getStatus(obj["status"]!!.jsonPrimitive.content)
            last_chapter_read = obj["num_chapters_read"]!!.jsonPrimitive.int
            score = obj["score"]!!.jsonPrimitive.int.toFloat()
        }
    }

    companion object {
        // Registered under arkon's MAL account
        private const val clientId = "8fd3313bc138e8b890551aa1de1a2589"

        private const val baseOAuthUrl = "https://myanimelist.net/v1/oauth2"
        private const val baseApiUrl = "https://api.myanimelist.net/v2"

        private const val listPaginationAmount = 250

        private var codeVerifier: String = ""

        fun authUrl(): Uri = "$baseOAuthUrl/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("code_challenge", getPkceChallengeCode())
            .appendQueryParameter("response_type", "code")
            .build()

        fun mangaUrl(id: Int): Uri = "$baseApiUrl/manga".toUri().buildUpon()
            .appendPath(id.toString())
            .appendPath("my_list_status")
            .build()

        fun refreshTokenRequest(refreshToken: String): Request {
            val formBody: RequestBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()
            return POST("$baseOAuthUrl/token", body = formBody)
        }

        private fun getPkceChallengeCode(): String {
            codeVerifier = PkceUtil.generateCodeVerifier()
            return codeVerifier
        }
    }
}
