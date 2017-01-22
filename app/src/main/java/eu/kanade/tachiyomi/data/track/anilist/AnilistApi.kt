package eu.kanade.tachiyomi.data.track.anilist

import android.net.Uri
import com.github.salomonbrys.kotson.int
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.network.POST
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import rx.Observable

class AnilistApi(val client: OkHttpClient, interceptor: AnilistInterceptor) {

    private val rest = restBuilder()
            .client(client.newBuilder().addInterceptor(interceptor).build())
            .build()
            .create(Rest::class.java)

    fun addLibManga(track: Track): Observable<Track> {
        return rest.addLibManga(track.remote_id, track.last_chapter_read, track.toAnilistStatus())
                .map { response ->
                    response.body().close()
                    if (!response.isSuccessful) {
                        throw Exception("Could not add manga")
                    }
                    track
                }
    }

    fun updateLibManga(track: Track): Observable<Track> {
        return rest.updateLibManga(track.remote_id, track.last_chapter_read, track.toAnilistStatus(),
                track.toAnilistScore())
                .map { response ->
                    response.body().close()
                    if (!response.isSuccessful) {
                        throw Exception("Could not update manga")
                    }
                    track
                }
    }

    fun search(query: String): Observable<List<Track>> {
        return rest.search(query, 1)
                .map { list ->
                    list.filter { it.type != "Novel" }.map { it.toTrack() }
                }
                .onErrorReturn { emptyList() }
    }

    fun getList(username: String): Observable<List<Track>> {
        return rest.getLib(username)
                .map { lib ->
                    lib.flatten().map { it.toTrack() }
                }
    }

    fun findLibManga(track: Track, username: String) : Observable<Track?> {
        // TODO avoid getting the entire list
        return getList(username)
                .map { list -> list.find { it.remote_id == track.remote_id } }
    }

    fun getLibManga(track: Track, username: String): Observable<Track> {
        return findLibManga(track, username)
                .map { it ?: throw Exception("Could not find manga") }
    }

    fun login(authCode: String): Observable<OAuth> {
        return restBuilder()
                .client(client)
                .build()
                .create(Rest::class.java)
                .requestAccessToken(authCode)
    }

    fun getCurrentUser(): Observable<Pair<String, Int>> {
        return rest.getCurrentUser()
                .map { it["id"].string to it["score_type"].int }
    }

    private fun restBuilder() = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())

    private interface Rest {

        @FormUrlEncoded
        @POST("auth/access_token")
        fun requestAccessToken(
                @Field("code") code: String,
                @Field("grant_type") grant_type: String = "authorization_code",
                @Field("client_id") client_id: String = clientId,
                @Field("client_secret") client_secret: String = clientSecret,
                @Field("redirect_uri") redirect_uri: String = clientUrl
        ) : Observable<OAuth>

        @GET("user")
        fun getCurrentUser(): Observable<JsonObject>

        @GET("manga/search/{query}")
        fun search(
                @Path("query") query: String,
                @Query("page") page: Int
        ): Observable<List<ALManga>>

        @GET("user/{username}/mangalist")
        fun getLib(
                @Path("username") username: String
        ): Observable<ALUserLists>

        @FormUrlEncoded
        @PUT("mangalist")
        fun addLibManga(
                @Field("id") id: Int,
                @Field("chapters_read") chapters_read: Int,
                @Field("list_status") list_status: String
        ) : Observable<Response<ResponseBody>>

        @FormUrlEncoded
        @PUT("mangalist")
        fun updateLibManga(
                @Field("id") id: Int,
                @Field("chapters_read") chapters_read: Int,
                @Field("list_status") list_status: String,
                @Field("score") score_raw: String
        ) : Observable<Response<ResponseBody>>

    }

    companion object {
        private const val clientId = "tachiyomi-hrtje"
        private const val clientSecret = "nlGB5OmgE9YWq5dr3gIDbTQV0C"
        private const val clientUrl = "tachiyomi://anilist-auth"
        private const val baseUrl = "https://anilist.co/api/"

        fun authUrl() = Uri.parse("${baseUrl}auth/authorize").buildUpon()
                .appendQueryParameter("grant_type", "authorization_code")
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", clientUrl)
                .appendQueryParameter("response_type", "code")
                .build()

        fun refreshTokenRequest(token: String) = POST("${baseUrl}auth/access_token",
                body = FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("client_id", clientId)
                        .add("client_secret", clientSecret)
                        .add("refresh_token", token)
                        .build())

    }

}