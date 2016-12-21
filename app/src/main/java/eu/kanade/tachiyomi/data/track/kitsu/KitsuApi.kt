package eu.kanade.tachiyomi.data.track.kitsu

import com.google.gson.JsonObject
import eu.kanade.tachiyomi.data.network.POST
import okhttp3.FormBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import rx.Observable

interface KitsuApi {

    companion object {
        private const val clientId = "dd031b32d2f56c990b1425efe6c42ad847e7fe3ab46bf1299f05ecd856bdb7dd"
        private const val clientSecret = "54d7307928f63414defd96399fc31ba847961ceaecef3a5fd93144e960c0e151"
        private const val baseUrl = "https://kitsu.io/api/edge/"
        private const val loginUrl = "https://kitsu.io/api/"

        fun createService(client: OkHttpClient) = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(KitsuApi::class.java)

        fun createLoginService(client: OkHttpClient) = Retrofit.Builder()
                .baseUrl(loginUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(KitsuApi::class.java)

        fun refreshTokenRequest(token: String) = POST("${loginUrl}oauth/token",
                body = FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("client_id", clientId)
                        .add("client_secret", clientSecret)
                        .add("refresh_token", token)
                        .build())
    }

    @FormUrlEncoded
    @POST("oauth/token")
    fun requestAccessToken(
            @Field("username") username: String,
            @Field("password") password: String,
            @Field("grant_type") grantType: String = "password",
            @Field("client_id") client_id: String = clientId,
            @Field("client_secret") client_secret: String = clientSecret
    ) : Observable<OAuth>

    @GET("users")
    fun getCurrentUser(
            @Query("filter[self]", encoded = true) self: Boolean = true
    ) : Observable<JsonObject>

    @GET("manga")
    fun search(
            @Query("filter[text]", encoded = true) query: String
    ): Observable<JsonObject>

    @GET("library-entries")
    fun getLibManga(
            @Query("filter[id]", encoded = true) remoteId: Int,
            @Query("include") includes: String = "media"
    ) : Observable<JsonObject>

    @GET("library-entries")
    fun findLibManga(
            @Query("filter[user_id]", encoded = true) userId: String,
            @Query("filter[media_id]", encoded = true) remoteId: Int,
            @Query("page[limit]", encoded = true) limit: Int = 10000,
            @Query("include") includes: String = "media"
    ) : Observable<JsonObject>

    @Headers("Content-Type: application/vnd.api+json")
    @POST("library-entries")
    fun addLibManga(
            @Body data: JsonObject
    ) : Observable<JsonObject>

    @Headers("Content-Type: application/vnd.api+json")
    @PATCH("library-entries/{id}")
    fun updateLibManga(
            @Path("id") remoteId: Int,
            @Body data: JsonObject
    ) : Observable<JsonObject>

}
