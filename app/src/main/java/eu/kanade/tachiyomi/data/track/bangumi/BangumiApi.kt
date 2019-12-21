package eu.kanade.tachiyomi.data.track.bangumi

import android.net.Uri
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.obj
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder

class BangumiApi(private val client: OkHttpClient, interceptor: BangumiInterceptor) {

  private val gson: Gson by injectLazy()
  private val parser = JsonParser()
  private val authClient = client.newBuilder().addInterceptor(interceptor).build()

  fun addLibManga(track: Track): Observable<Track> {
    val body = FormBody.Builder()
      .add("rating", track.score.toInt().toString())
      .add("status", track.toBangumiStatus())
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

  fun updateLibManga(track: Track): Observable<Track> {
    // chapter update
    val body = FormBody.Builder()
      .add("watched_eps", track.last_chapter_read.toString())
      .build()
    val request = Request.Builder()
      .url("$apiUrl/subject/${track.media_id}/update/watched_eps")
      .post(body)
      .build()

    // read status update
    val sbody = FormBody.Builder()
      .add("status", track.toBangumiStatus())
      .build()
    val srequest = Request.Builder()
      .url("$apiUrl/collection/${track.media_id}/update")
      .post(sbody)
      .build()
    return authClient.newCall(request)
      .asObservableSuccess()
      .map {
        track
      }.flatMap {
        authClient.newCall(srequest)
          .asObservableSuccess()
          .map {
            track
          }
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
        val response = parser.parse(responseBody).obj["list"]?.array
        response?.filter { it.obj["type"].asInt == 1 }?.map { jsonToSearch(it.obj) }
      }

  }

  private fun jsonToSearch(obj: JsonObject): TrackSearch {
    return TrackSearch.create(TrackManager.BANGUMI).apply {
      media_id = obj["id"].asInt
      title = obj["name_cn"].asString
      cover_url = obj["images"].obj["common"].asString
      summary = obj["name"].asString
      tracking_url = obj["url"].asString
    }
  }

  private fun jsonToTrack(mangas: JsonObject): Track {
    return Track.create(TrackManager.BANGUMI).apply {
      title = mangas["name"].asString
      media_id = mangas["id"].asInt
      score = if (mangas["rating"] != null)
        (if (mangas["rating"].isJsonObject) mangas["rating"].obj["score"].asFloat else 0f)
      else 0f
      status = Bangumi.DEFAULT_STATUS
      tracking_url = mangas["url"].asString
    }
  }

  fun findLibManga(track: Track): Observable<Track?> {
    val urlMangas = "$apiUrl/subject/${track.media_id}"
    val requestMangas = Request.Builder()
      .url(urlMangas)
      .get()
      .build()

    return authClient.newCall(requestMangas)
      .asObservableSuccess()
      .map { netResponse ->
        // get comic info
        val responseBody = netResponse.body()?.string().orEmpty()
        jsonToTrack(parser.parse(responseBody).obj)
      }
  }

  fun statusLibManga(track: Track): Observable<Track?> {
    val urlUserRead = "$apiUrl/collection/${track.media_id}"
    val requestUserRead = Request.Builder()
      .url(urlUserRead)
      .cacheControl(CacheControl.FORCE_NETWORK)
      .get()
      .build()

    // todo get user readed chapter here
    return authClient.newCall(requestUserRead)
      .asObservableSuccess()
      .map { netResponse ->
        val resp = netResponse.body()?.string()
        val coll = gson.fromJson(resp, Collection::class.java)
        track.status = coll.status?.id!!
        track.last_chapter_read = coll.ep_status!!
        track
      }
  }

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
