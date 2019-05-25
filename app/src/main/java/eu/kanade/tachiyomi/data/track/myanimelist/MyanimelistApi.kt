package eu.kanade.tachiyomi.data.track.myanimelist

import android.net.Uri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.util.selectInt
import eu.kanade.tachiyomi.util.selectText
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import rx.Observable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream


class MyanimelistApi(private val client: OkHttpClient, interceptor: MyAnimeListInterceptor) {

    private val malClient = client.newBuilder().addInterceptor(interceptor).build()

    fun addLibManga(track: Track): Observable<Track> {
        return Observable.defer {
            malClient.newCall(POST(url = getAddUrl(), body = getMangaPostPayload(track)))
                    .asObservableSuccess()
                    .map { track }
        }
    }

    fun updateLibManga(track: Track): Observable<Track> {
        return Observable.defer {
            malClient.newCall(POST(url = getUpdateUrl(), body = getMangaPostPayload(track)))
                    .asObservableSuccess()
                    .map { track }
        }
    }

    fun search(query: String): Observable<List<TrackSearch>> {
        return malClient.newCall(GET(getSearchUrl(query)))
                .asObservable()
                .flatMap { response ->
                    Observable.from(Jsoup.parse(response.consumeBody())
                            .select("div.js-categories-seasonal.js-block-list.list")
                            .select("table").select("tbody")
                            .select("tr").drop(1))
                }
                .filter { row ->
                    row.select(TD)[2].text() != "Novel"
                }
                .map { row ->
                    TrackSearch.create(TrackManager.MYANIMELIST).apply {
                        title = row.searchTitle()
                        media_id = row.searchMediaId()
                        total_chapters = row.searchTotalChapters()
                        summary = row.searchSummary()
                        cover_url = row.searchCoverUrl()
                        tracking_url = mangaUrl(media_id)
                        publishing_status = row.searchPublishingStatus()
                        publishing_type = row.searchPublishingType()
                        start_date = row.searchStartDate()
                    }
                }
                .toList()
    }

    private fun getList(): Observable<List<TrackSearch>> {
        return getListUrl()
                .flatMap { url ->
                    getListXml(url)
                }
                .flatMap { doc ->
                    Observable.from(doc.select("manga"))
                }
                .map {
                    TrackSearch.create(TrackManager.MYANIMELIST).apply {
                        title = it.selectText("manga_title")!!
                        media_id = it.selectInt("manga_mangadb_id")
                        last_chapter_read = it.selectInt("my_read_chapters")
                        status = getStatus(it.selectText("my_status")!!)
                        score = it.selectInt("my_score").toFloat()
                        total_chapters = it.selectInt("manga_chapters")
                        tracking_url = mangaUrl(media_id)
                    }
                }
                .toList()
    }

    private fun getListXml(url: String): Observable<Document> {
        return malClient.newCall(GET(url))
                .asObservable()
                .map { response ->
                    Jsoup.parse(response.consumeXmlBody(), "", Parser.xmlParser())
                }
    }

    fun findLibManga(track: Track): Observable<Track?> {
        return getList()
                .map { list -> list.find { it.media_id == track.media_id } }
    }

    fun getLibManga(track: Track): Observable<Track> {
        return findLibManga(track)
                .map { it ?: throw Exception("Could not find manga") }
    }

    fun login(username: String, password: String): Observable<String> {
        return getSessionInfo()
                .flatMap { csrf ->
                    login(username, password, csrf)
                }
    }

    private fun getSessionInfo(): Observable<String> {
        return client.newCall(GET(getLoginUrl()))
                .asObservable()
                .map { response ->
                    Jsoup.parse(response.consumeBody())
                            .select("meta[name=csrf_token]")
                            .attr("content")
                }
    }

    private fun login(username: String, password: String, csrf: String): Observable<String> {
        return client.newCall(POST(url = getLoginUrl(), body = getLoginPostBody(username, password, csrf)))
                .asObservable()
                .map { response ->
                    response.use {
                        if (response.priorResponse()?.code() != 302) throw Exception("Authentication error")
                    }
                    csrf
                }
    }

    private fun getLoginPostBody(username: String, password: String, csrf: String): RequestBody {
        return FormBody.Builder()
                .add("user_name", username)
                .add("password", password)
                .add("cookie", "1")
                .add("sublogin", "Login")
                .add("submit", "1")
                .add(CSRF, csrf)
                .build()
    }

    private fun getExportPostBody(): RequestBody {
        return FormBody.Builder()
                .add("type", "2")
                .add("subexport", "Export My List")
                .build()
    }

    private fun getMangaPostPayload(track: Track): RequestBody {
        val body = JSONObject()
                .put("manga_id", track.media_id)
                .put("status", track.status)
                .put("score", track.score)
                .put("num_read_chapters", track.last_chapter_read)

        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString())
    }

    private fun getLoginUrl() = Uri.parse(baseUrl).buildUpon()
            .appendPath("login.php")
            .toString()

    private fun getSearchUrl(query: String): String {
        val col = "c[]"
        return Uri.parse(baseUrl).buildUpon()
                .appendPath("manga.php")
                .appendQueryParameter("q", query)
                .appendQueryParameter(col, "a")
                .appendQueryParameter(col, "b")
                .appendQueryParameter(col, "c")
                .appendQueryParameter(col, "d")
                .appendQueryParameter(col, "e")
                .appendQueryParameter(col, "g")
                .toString()
    }

    private fun getExportListUrl() = Uri.parse(baseUrl).buildUpon()
            .appendPath("panel.php")
            .appendQueryParameter("go", "export")
            .toString()

    private fun getListUrl(): Observable<String> {
        return malClient.newCall(POST(url = getExportListUrl(), body = getExportPostBody()))
                .asObservable()
                .map {response ->
                    baseUrl + Jsoup.parse(response.consumeBody())
                            .select("div.goodresult")
                            .select("a")
                            .attr("href")
                }
    }

    private fun getUpdateUrl() = Uri.parse(baseModifyListUrl).buildUpon()
            .appendPath("edit.json")
            .toString()

    private fun getAddUrl() = Uri.parse(baseModifyListUrl).buildUpon()
            .appendPath( "add.json")
            .toString()
    
    private fun Response.consumeBody(): String? {
        use {
            if (it.code() != 200) throw Exception("Login error")
            return it.body()?.string()
        }
    }

    private fun Response.consumeXmlBody(): String? {
        use { res ->
            if (res.code() != 200) throw Exception("Export list error")
            BufferedReader(InputStreamReader(GZIPInputStream(res.body()?.source()?.inputStream()))).use { reader ->
                val sb = StringBuilder()
                reader.forEachLine { line ->
                    sb.append(line)
                }
                return sb.toString()
            }
        }
    }

    companion object {
        const val baseUrl = "https://myanimelist.net"
        private const val baseMangaUrl = "$baseUrl/manga/"
        private const val baseModifyListUrl = "$baseUrl/ownlist/manga"

        fun mangaUrl(remoteId: Int) = baseMangaUrl + remoteId

        fun Element.searchTitle() = select("strong").text()!!

        fun Element.searchTotalChapters() = if (select(TD)[4].text() == "-") 0 else select(TD)[4].text().toInt()

        fun Element.searchCoverUrl() = select("img")
                .attr("data-src")
                .split("\\?")[0]
                .replace("/r/50x70/", "/")

        fun Element.searchMediaId() = select("div.picSurround")
                .select("a").attr("id")
                .replace("sarea", "")
                .toInt()

        fun Element.searchSummary() = select("div.pt4")
                .first()
                .ownText()!!

        fun Element.searchPublishingStatus() = if (select(TD).last().text() == "-") PUBLISHING else FINISHED

        fun Element.searchPublishingType() = select(TD)[2].text()!!

        fun Element.searchStartDate() = select(TD)[6].text()!!

        fun getStatus(status: String) = when (status) {
            "Reading" -> 1
            "Completed" -> 2
            "On-Hold" -> 3
            "Dropped" -> 4
            "Plan to Read" -> 6
            else -> 1
            }

        const val CSRF = "csrf_token"
        const val TD = "td"
        private const val FINISHED = "Finished"
        private const val PUBLISHING = "Publishing"
    }
}