package eu.kanade.tachiyomi.data.track.kitsu

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

class KitsuApi(private val client: OkHttpClient, interceptor: KitsuInterceptor) {

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    private val rest = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(authClient)
        .addConverterFactory(jsonConverter)
        .build()
        .create(Rest::class.java)

    private val searchRest = Retrofit.Builder()
        .baseUrl(algoliaKeyUrl)
        .client(authClient)
        .addConverterFactory(jsonConverter)
        .build()
        .create(SearchKeyRest::class.java)

    private val algoliaRest = Retrofit.Builder()
        .baseUrl(algoliaUrl)
        .client(client)
        .addConverterFactory(jsonConverter)
        .build()
        .create(AgoliaSearchRest::class.java)

    suspend fun addLibManga(track: Track, userId: String): Track {
        val data = buildJsonObject {
            putJsonObject("data") {
                put("type", "libraryEntries")
                putJsonObject("attributes") {
                    put("status", track.toKitsuStatus())
                    put("progress", track.last_chapter_read)
                }
                putJsonObject("relationships") {
                    putJsonObject("user") {
                        putJsonObject("data") {
                            put("id", userId)
                            put("type", "users")
                        }
                    }
                    putJsonObject("media") {
                        putJsonObject("data") {
                            put("id", track.media_id)
                            put("type", "manga")
                        }
                    }
                }
            }
        }

        val json = rest.addLibManga(data)
        track.media_id = json["data"]!!.jsonObject["id"]!!.jsonPrimitive.int
        return track
    }

    suspend fun updateLibManga(track: Track): Track {
        val data = buildJsonObject {
            putJsonObject("data") {
                put("type", "libraryEntries")
                put("id", track.media_id)
                putJsonObject("attributes") {
                    put("status", track.toKitsuStatus())
                    put("progress", track.last_chapter_read)
                    put("ratingTwenty", track.toKitsuScore())
                }
            }
        }

        rest.updateLibManga(track.media_id, data)
        return track
    }

    suspend fun search(query: String): List<TrackSearch> {
        val json = searchRest.getKey()
        val key = json["media"]!!.jsonObject["key"]!!.jsonPrimitive.content
        return algoliaSearch(key, query)
    }

    private suspend fun algoliaSearch(key: String, query: String): List<TrackSearch> {
        val jsonObject = buildJsonObject {
            put("params", "query=$query$algoliaFilter")
        }
        val json = algoliaRest.getSearchQuery(algoliaAppId, key, jsonObject)
        val data = json["hits"]!!.jsonArray
        return data.map { KitsuSearchManga(it.jsonObject) }
            .filter { it.subType != "novel" }
            .map { it.toTrack() }
    }

    suspend fun findLibManga(track: Track, userId: String): Track? {
        val json = rest.findLibManga(track.media_id, userId)
        val data = json["data"]!!.jsonArray
        return if (data.size > 0) {
            val manga = json["included"]!!.jsonArray[0].jsonObject
            KitsuLibManga(data[0].jsonObject, manga).toTrack()
        } else {
            null
        }
    }

    suspend fun getLibManga(track: Track): Track {
        val json = rest.getLibManga(track.media_id)
        val data = json["data"]!!.jsonArray
        return if (data.size > 0) {
            val manga = json["included"]!!.jsonArray[0].jsonObject
            KitsuLibManga(data[0].jsonObject, manga).toTrack()
        } else {
            throw Exception("Could not find manga")
        }
    }

    suspend fun login(username: String, password: String): OAuth {
        return Retrofit.Builder()
            .baseUrl(loginUrl)
            .client(client)
            .addConverterFactory(jsonConverter)
            .build()
            .create(LoginRest::class.java)
            .requestAccessToken(username, password)
    }

    suspend fun getCurrentUser(): String {
        return rest.getCurrentUser()["data"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.content
    }

    private interface Rest {

        @Headers("Content-Type: application/vnd.api+json")
        @POST("library-entries")
        suspend fun addLibManga(
            @Body data: JsonObject
        ): JsonObject

        @Headers("Content-Type: application/vnd.api+json")
        @PATCH("library-entries/{id}")
        suspend fun updateLibManga(
            @Path("id") remoteId: Int,
            @Body data: JsonObject
        ): JsonObject

        @GET("library-entries")
        suspend fun findLibManga(
            @Query("filter[manga_id]", encoded = true) remoteId: Int,
            @Query("filter[user_id]", encoded = true) userId: String,
            @Query("include") includes: String = "manga"
        ): JsonObject

        @GET("library-entries")
        suspend fun getLibManga(
            @Query("filter[id]", encoded = true) remoteId: Int,
            @Query("include") includes: String = "manga"
        ): JsonObject

        @GET("users")
        suspend fun getCurrentUser(
            @Query("filter[self]", encoded = true) self: Boolean = true
        ): JsonObject
    }

    private interface SearchKeyRest {
        @GET("media/")
        suspend fun getKey(): JsonObject
    }

    private interface AgoliaSearchRest {
        @POST("query/")
        suspend fun getSearchQuery(@Header("X-Algolia-Application-Id") appid: String, @Header("X-Algolia-API-Key") key: String, @Body json: JsonObject): JsonObject
    }

    private interface LoginRest {

        @FormUrlEncoded
        @POST("oauth/token")
        suspend fun requestAccessToken(
            @Field("username") username: String,
            @Field("password") password: String,
            @Field("grant_type") grantType: String = "password",
            @Field("client_id") client_id: String = clientId,
            @Field("client_secret") client_secret: String = clientSecret
        ): OAuth
    }

    companion object {
        private const val clientId = "dd031b32d2f56c990b1425efe6c42ad847e7fe3ab46bf1299f05ecd856bdb7dd"
        private const val clientSecret = "54d7307928f63414defd96399fc31ba847961ceaecef3a5fd93144e960c0e151"
        private const val baseUrl = "https://kitsu.io/api/edge/"
        private const val loginUrl = "https://kitsu.io/api/"
        private const val baseMangaUrl = "https://kitsu.io/manga/"
        private const val algoliaKeyUrl = "https://kitsu.io/api/edge/algolia-keys/"
        private const val algoliaUrl = "https://AWQO5J657S-dsn.algolia.net/1/indexes/production_media/"
        private const val algoliaAppId = "AWQO5J657S"
        private const val algoliaFilter = "&facetFilters=%5B%22kind%3Amanga%22%5D&attributesToRetrieve=%5B%22synopsis%22%2C%22canonicalTitle%22%2C%22chapterCount%22%2C%22posterImage%22%2C%22startDate%22%2C%22subtype%22%2C%22endDate%22%2C%20%22id%22%5D"

        private val jsonConverter = Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())

        fun mangaUrl(remoteId: Int): String {
            return baseMangaUrl + remoteId
        }

        fun refreshTokenRequest(token: String) = POST(
            "${loginUrl}oauth/token",
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", token)
                .build()
        )
    }
}
