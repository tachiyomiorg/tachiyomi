package eu.kanade.tachiyomi.data.track.kitsu

import com.github.salomonbrys.kotson.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import okhttp3.FormBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import rx.Observable

class KitsuApi(private val client: OkHttpClient, interceptor: KitsuInterceptor) {

    private val rest = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client.newBuilder().addInterceptor(interceptor).build())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build()
            .create(KitsuApi.Rest::class.java)

    fun addLibManga(track: Track, userId: String): Observable<Track> {
        return Observable.defer {
            // @formatter:off
            val data = jsonObject(
                    "type" to "libraryEntries",
                    "attributes" to jsonObject(
                            "status" to track.toKitsuStatus(),
                            "progress" to track.last_chapter_read
                    ),
                    "relationships" to jsonObject(
                            "user" to jsonObject(
                                    "data" to jsonObject(
                                            "id" to userId,
                                            "type" to "users"
                                    )
                            ),
                            "media" to jsonObject(
                                    "data" to jsonObject(
                                            "id" to track.media_id,
                                            "type" to "manga"
                                    )
                            )
                    )
            )
            // @formatter:on

            rest.addLibManga(jsonObject("data" to data))
                    .map { json ->
                        track.media_id = json["data"]["id"].int
                        track
                    }
        }
    }

    fun updateLibManga(track: Track): Observable<Track> {
        return Observable.defer {
            // @formatter:off
            val data = jsonObject(
                    "type" to "libraryEntries",
                    "id" to track.media_id,
                    "attributes" to jsonObject(
                            "status" to track.toKitsuStatus(),
                            "progress" to track.last_chapter_read,
                            "ratingTwenty" to track.toKitsuScore()
                    )
            )
            // @formatter:on

            rest.updateLibManga(track.media_id, jsonObject("data" to data))
                    .map { track }
        }
    }

    fun search(query: String): Observable<List<TrackSearch>> {
        return rest.search(query)
                .map { json ->
                    val data = json["data"].array
                    data.map { KitsuManga(it.obj) }
                            .filter { it.type != "novel" }
                            .map { it.toTrack() }
                }
    }

    fun findLibManga(track: Track, userId: String): Observable<Track?> {
        return rest.findLibManga(track.media_id, userId)
                .map { json ->
                    val data = json["data"].array
                    if (data.size() > 0) {
                        val manga = json["included"].array[0].obj
                        KitsuLibManga(data[0].obj, manga).toTrack()
                    } else {
                        null
                    }
                }
    }

    fun getLibManga(track: Track): Observable<Track> {
        return rest.getLibManga(track.media_id)
                .map { json ->
                    val data = json["data"].array
                    if (data.size() > 0) {
                        val manga = json["included"].array[0].obj
                        KitsuLibManga(data[0].obj, manga).toTrack()
                    } else {
                        throw Exception("Could not find manga")
                    }
                }
    }

    fun login(username: String, password: String): Observable<OAuth> {
        return Retrofit.Builder()
                .baseUrl(loginUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(KitsuApi.LoginRest::class.java)
                .requestAccessToken(username, password)
    }

    fun getCurrentUser(): Observable<String> {
        return rest.getCurrentUser().map { it["data"].array[0]["id"].string }
    }

    private interface Rest {

        @Headers("Content-Type: application/vnd.api+json")
        @POST("library-entries")
        fun addLibManga(
                @Body data: JsonObject
        ): Observable<JsonObject>

        @Headers("Content-Type: application/vnd.api+json")
        @PATCH("library-entries/{id}")
        fun updateLibManga(
                @Path("id") remoteId: Int,
                @Body data: JsonObject
        ): Observable<JsonObject>

        @GET("manga")
        fun search(
                @Query("filter[text]", encoded = true) query: String
        ): Observable<JsonObject>

        @GET("library-entries")
        fun findLibManga(
                @Query("filter[manga_id]", encoded = true) remoteId: Int,
                @Query("filter[user_id]", encoded = true) userId: String,
                @Query("include") includes: String = "manga"
        ): Observable<JsonObject>

        @GET("library-entries")
        fun getLibManga(
                @Query("filter[id]", encoded = true) remoteId: Int,
                @Query("include") includes: String = "manga"
        ): Observable<JsonObject>

        @GET("users")
        fun getCurrentUser(
                @Query("filter[self]", encoded = true) self: Boolean = true
        ): Observable<JsonObject>

    }

    private interface LoginRest {

        @FormUrlEncoded
        @POST("oauth/token")
        fun requestAccessToken(
                @Field("username") username: String,
                @Field("password") password: String,
                @Field("grant_type") grantType: String = "password",
                @Field("client_id") client_id: String = clientId,
                @Field("client_secret") client_secret: String = clientSecret
        ): Observable<OAuth>

    }

    companion object {
        private const val clientId = "dd031b32d2f56c990b1425efe6c42ad847e7fe3ab46bf1299f05ecd856bdb7dd"
        private const val clientSecret = "54d7307928f63414defd96399fc31ba847961ceaecef3a5fd93144e960c0e151"
        private const val baseUrl = "https://kitsu.io/api/edge/"
        private const val loginUrl = "https://kitsu.io/api/"
        private const val baseMangaUrl = "https://kitsu.io/manga/"

        fun mangaUrl(remoteId: Int): String {
            return baseMangaUrl + remoteId
        }


        fun refreshTokenRequest(token: String) = POST("${loginUrl}oauth/token",
                body = FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("client_id", clientId)
                        .add("client_secret", clientSecret)
                        .add("refresh_token", token)
                        .build())

    }

}
