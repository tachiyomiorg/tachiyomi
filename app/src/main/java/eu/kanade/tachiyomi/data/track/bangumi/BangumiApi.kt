package eu.kanade.tachiyomi.data.track.bangumi

import android.net.Uri
import android.util.Log
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.obj
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi.Companion.STATUS
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder

class BangumiApi(private val client: OkHttpClient, interceptor: BangumiInterceptor) {

  private val gson: Gson by injectLazy()
  private val parser = JsonParser()
  private val jsonime = MediaType.parse("application/json; charset=utf-8")
  private val authClient = client.newBuilder().addInterceptor(interceptor).build()

  fun addLibManga(track: Track, user_id: String): Observable<Track> {
    val body = FormBody.Builder()
      .add("rating", track.score.toInt().toString())
      .add("status", STATUS[track.status])
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

  fun updateLibManga(track: Track, user_id: String): Observable<Track> {
    val body = FormBody.Builder()
      .add("watched_eps", track.last_chapter_read.toString())
      .build()
    val request = Request.Builder()
      .url("$apiUrl/subject/${track.media_id}/update/watched_eps")
      .post(body)
      .build()
    return authClient.newCall(request)
      .asObservableSuccess()
      .map {
        track
      }
  }

  fun search(search: String): Observable<List<TrackSearch>> {
    val url = Uri.parse(
      "$apiUrl/search/subject/${URLEncoder.encode(search, Charsets.UTF_8.name())}").buildUpon()
      .appendQueryParameter("max_results", "20")
      .build()
    val request = Request.Builder()
      .url(url.toString())
      .get()
      .build()
    return authClient.newCall(request)
      .asObservableSuccess()
      .map { netResponse ->
        val responseBody = netResponse.body()?.string().orEmpty()
        if (responseBody.isEmpty()) {
          throw Exception("Null Response")
        }
//                    parser.parse(responseBody).obj["results"].asInt
        val response = parser.parse(responseBody).obj["list"]?.array
        response?.map { jsonToSearch(it.obj) }
      }

  }

  private fun jsonToSearch(obj: JsonObject): TrackSearch {
    return TrackSearch.create(TrackManager.BANGUMI).apply {
      //            val images = obj["images"].obj
      media_id = obj["id"].asInt
      title = obj["name"].asString
//            total_chapters = obj["chapters"].asInt
      cover_url = obj["images"].obj["common"].asString
      summary = obj["name_cn"].asString
      tracking_url = obj["url"].asString
//            publishing_status = obj["status"].asString
//            publishing_type = obj["kind"].asString
//            start_date = obj.get("aired_on").nullString.orEmpty()
    }
  }

  private fun jsonToTrack(mangas: JsonObject): Track {
    return Track.create(TrackManager.BANGUMI).apply {
      title = mangas["name"].asString
      media_id = mangas["id"].asInt
      total_chapters = mangas["eps_count"].asInt
//            last_chapter_read = obj["chapters"].asInt
      last_chapter_read = 0
      score = if (mangas["rating"] != null)
        (if (mangas["rating"].isJsonObject) mangas["rating"].obj["score"].asFloat else 0f)
      else 0f
      status = 0
      tracking_url = mangas["url"].asString
    }
  }

  fun findLibManga(track: Track, user_id: String): Observable<Track?> {
    val url = Uri.parse("$apiUrl/collection/${track.media_id}").buildUpon()
      .appendQueryParameter("source", "onAir")
      .build()
    val request = Request.Builder()
      .url(url.toString())
      .post(FormBody.Builder().build())
      .build()

    val urlMangas = Uri.parse("$apiUrl/subject/${track.media_id}").buildUpon()
//                .appendQueryParameter("responseGroup", "large")
      .build()
    val requestMangas = Request.Builder()
      .url(urlMangas.toString())
      .get()
      .build()
    return authClient.newCall(requestMangas)
      .asObservableSuccess()
      .map { netResponse ->
        val responseBody = netResponse.body()?.string().orEmpty()
        jsonToTrack(parser.parse(responseBody).obj)
      }
  }

//    fun getCurrentUser(token: String): Int {
//        val user = client.newCall(GET("$apiUrl/oauth/token_status")).execute().body()?.string()
//        Log.i("FEILONG", user)
//        return parser.parse(user).obj["user_id"].asInt
//    }

  fun accessToken(code: String): Observable<OAuth> {
    return client.newCall(accessTokenRequest(code)).asObservableSuccess().map { netResponse ->
      val responseBody = netResponse.body()?.string().orEmpty()
      if (responseBody.isEmpty()) {
        throw Exception("Null Response")
      }
      gson.fromJson(responseBody, OAuth::class.java)
    }
  }

  private fun accessTokenRequest(code: String) = POST(oauthUrl,
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

    private const val baseUrl = "https://bangumi.org"
    private const val apiUrl = "https://api.bgm.tv"
    private const val oauthUrl = "https://bgm.tv/oauth/access_token"
    private const val loginUrl = "https://bgm.tv/oauth/authorize"

    private const val redirectUrl = "tachiyomi://bangumi-auth"
    private const val baseMangaUrl = "$apiUrl/mangas"

    fun mangaUrl(remoteId: Int): String {
      return "$baseMangaUrl/$remoteId"
    }

    fun authUrl() =
      Uri.parse(loginUrl).buildUpon()
        .appendQueryParameter("client_id", clientId)
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("redirect_uri", redirectUrl)
        .build()

    fun refreshTokenRequest(token: String) = POST(oauthUrl,
      body = FormBody.Builder()
        .add("grant_type", "refresh_token")
        .add("client_id", clientId)
        .add("client_secret", clientSecret)
        .add("refresh_token", token)
        .add("redirect_uri", redirectUrl)
        .build())

  }

}
