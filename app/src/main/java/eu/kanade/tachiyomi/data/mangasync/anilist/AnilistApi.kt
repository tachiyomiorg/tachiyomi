package eu.kanade.tachiyomi.data.mangasync.anilist

import android.net.Uri
import com.google.gson.JsonObject
import eu.kanade.tachiyomi.data.mangasync.anilist.model.ALManga
import eu.kanade.tachiyomi.data.mangasync.anilist.model.ALUserLists
import eu.kanade.tachiyomi.data.mangasync.anilist.model.OAuth
import eu.kanade.tachiyomi.data.network.POST
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import rx.Observable

interface AnilistApi {

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

        fun createService(client: OkHttpClient) = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(AnilistApi::class.java)

    }

    @FormUrlEncoded
    @POST("auth/access_token")
    fun requestAccessToken(
            @Field("code") code: String,
            @Field("grant_type") grant_type: String = "authorization_code",
            @Field("client_id") client_id: String = clientId,
            @Field("client_secret") client_secret: String = clientSecret,
            @Field("redirect_uri") redirect_uri: String = clientUrl)
            : Observable<OAuth>

    @GET("user")
    fun getCurrentUser(): Observable<JsonObject>

    @GET("manga/search/{query}")
    fun search(@Path("query") query: String, @Query("page") page: Int): Observable<List<ALManga>>

    @GET("user/{username}/mangalist")
    fun getList(@Path("username") username: String): Observable<ALUserLists>

    @FormUrlEncoded
    @PUT("mangalist")
    fun addManga(
            @Field("id") id: Int,
            @Field("chapters_read") chapters_read: Int,
            @Field("list_status") list_status: String)
            : Observable<Response<ResponseBody>>

    @FormUrlEncoded
    @PUT("mangalist")
    fun updateManga(
            @Field("id") id: Int,
            @Field("chapters_read") chapters_read: Int,
            @Field("list_status") list_status: String,
            @Field("score") score_raw: String)
            : Observable<Response<ResponseBody>>

}