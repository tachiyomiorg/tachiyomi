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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.zip.GZIPInputStream
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import rx.Observable

class MyAnimeListApi(private val client: OkHttpClient, interceptor: MyAnimeListInterceptor) {

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    fun search(query: String): Observable<List<TrackSearch>> {
        return if (query.startsWith(PREFIX_MY)) {
            val realQuery = query.removePrefix(PREFIX_MY)
            getList()
                    .flatMap { Observable.from(it) }
                    .filter { it.title.contains(realQuery, true) }
                    .toList()
        } else {
            client.newCall(GET(searchUrl(query)))
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
    }

    fun addLibManga(track: Track): Observable<Track> {
        return Observable.defer {
            authClient.newCall(POST(url = addUrl(), body = mangaPostPayload(track)))
                    .asObservableSuccess()
                    .map { track }
        }
    }

    fun updateLibManga(track: Track): Observable<Track> {
        return Observable.defer {
            authClient.newCall(POST(url = updateUrl(), body = mangaPostPayload(track)))
                    .asObservableSuccess()
                    .map { track }
        }
    }

    fun findLibManga(track: Track): Observable<Track?> {
        return authClient.newCall(GET(url = listEntryUrl(track.media_id)))
                .asObservable()
                .map { response ->
                    var libTrack: Track? = null
                    response.use {
                        if (it.priorResponse?.isRedirect != true) {
                            val trackForm = Jsoup.parse(it.consumeBody())

                            val startMonth = trackForm.select("#add_manga_start_date_month > option[selected]").`val`().toIntOrNull()
                            val startDay = trackForm.select("#add_manga_start_date_day > option[selected]").`val`().toIntOrNull()
                            val startYear = trackForm.select("#add_manga_start_date_year > option[selected]").`val`().toIntOrNull()

                            val finishMonth = trackForm.select("#add_manga_finish_date_month > option[selected]").`val`().toIntOrNull()
                            val finishDay = trackForm.select("#add_manga_finish_date_day > option[selected]").`val`().toIntOrNull()
                            val finishYear = trackForm.select("#add_manga_finish_date_year > option[selected]").`val`().toIntOrNull()

                            val startDate =
                                    if (startYear != null && startMonth != null && startDay != null)
                                        GregorianCalendar(startYear, startMonth, startDay)
                                    else
                                        null

                            val finishDate =
                                    if (finishYear != null && finishMonth != null && finishDay != null)
                                        GregorianCalendar(finishYear, finishMonth, finishDay)
                                    else
                                        null

                            libTrack = Track.create(TrackManager.MYANIMELIST).apply {
                                last_chapter_read = trackForm.select("#add_manga_num_read_chapters").`val`().toInt()
                                total_chapters = trackForm.select("#totalChap").text().toInt()
                                status = trackForm.select("#add_manga_status > option[selected]").`val`().toInt()
                                score = trackForm.select("#add_manga_score > option[selected]").`val`().toFloatOrNull()
                                        ?: 0f
                                started_reading_date = startDate
                                finished_reading_date = finishDate
                            }
                        }
                    }
                    libTrack
                }
    }

    fun getLibManga(track: Track): Observable<Track> {
        return findLibManga(track)
                .map { it ?: throw Exception("Could not find manga") }
    }

    fun login(username: String, password: String): String {
        val csrf = getSessionInfo()

        login(username, password, csrf)

        return csrf
    }

    private fun getSessionInfo(): String {
        val response = client.newCall(GET(loginUrl())).execute()

        return Jsoup.parse(response.consumeBody())
                .select("meta[name=csrf_token]")
                .attr("content")
    }

    private fun login(username: String, password: String, csrf: String) {
        val response = client.newCall(POST(url = loginUrl(), body = loginPostBody(username, password, csrf))).execute()

        response.use {
            if (response.priorResponse?.code != 302) throw Exception("Authentication error")
        }
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
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                    val startDateStr = it.selectText("my_start_date")
                    val startDate =
                            if (!startDateStr.isNullOrBlank() && startDateStr != "0000-00-00") {
                                val date = dateFormat.parse(startDateStr) // Can this throw?
                                if (date != null) {
                                    val cal = GregorianCalendar.getInstance()
                                    cal.time = date
                                    cal
                                } else null
                            } else null

                    val finishDateStr = it.selectText("my_finish_date")
                    val finishDate =
                            if (!finishDateStr.isNullOrBlank() && finishDateStr != "0000-00-00") {
                                val date = dateFormat.parse(finishDateStr) // Can this throw?
                                if (date != null) {
                                    val cal = GregorianCalendar.getInstance()
                                    cal.time = date
                                    cal
                                } else null
                            } else null

                    TrackSearch.create(TrackManager.MYANIMELIST).apply {
                        title = it.selectText("manga_title")!!
                        media_id = it.selectInt("manga_mangadb_id")
                        last_chapter_read = it.selectInt("my_read_chapters")
                        status = getStatus(it.selectText("my_status")!!)
                        score = it.selectInt("my_score").toFloat()
                        total_chapters = it.selectInt("manga_chapters")
                        tracking_url = mangaUrl(media_id)
                        started_reading_date = startDate
                        finished_reading_date = finishDate
                    }
                }
                .toList()
    }

    private fun getListUrl(): Observable<String> {
        return authClient.newCall(POST(url = exportListUrl(), body = exportPostBody()))
                .asObservable()
                .map { response ->
                    baseUrl + Jsoup.parse(response.consumeBody())
                            .select("div.goodresult")
                            .select("a")
                            .attr("href")
                }
    }

    private fun getListXml(url: String): Observable<Document> {
        return authClient.newCall(GET(url))
                .asObservable()
                .map { response ->
                    Jsoup.parse(response.consumeXmlBody(), "", Parser.xmlParser())
                }
    }

    private fun Response.consumeBody(): String? {
        use {
            if (it.code != 200) throw Exception("HTTP error ${it.code}")
            return it.body?.string()
        }
    }

    private fun Response.consumeXmlBody(): String? {
        use { res ->
            if (res.code != 200) throw Exception("Export list error")
            BufferedReader(InputStreamReader(GZIPInputStream(res.body?.source()?.inputStream()))).use { reader ->
                val sb = StringBuilder()
                reader.forEachLine { line ->
                    sb.append(line)
                }
                return sb.toString()
            }
        }
    }

    companion object {
        const val CSRF = "csrf_token"

        private const val baseUrl = "https://myanimelist.net"
        private const val baseMangaUrl = "$baseUrl/manga/"
        private const val baseModifyListUrl = "$baseUrl/ownlist/manga"
        private const val PREFIX_MY = "my:"
        private const val TD = "td"

        private fun mangaUrl(remoteId: Int) = baseMangaUrl + remoteId

        private fun loginUrl() = Uri.parse(baseUrl).buildUpon()
                .appendPath("login.php")
                .toString()

        private fun searchUrl(query: String): String {
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

        private fun exportListUrl() = Uri.parse(baseUrl).buildUpon()
                .appendPath("panel.php")
                .appendQueryParameter("go", "export")
                .toString()

        private fun updateUrl() = Uri.parse(baseModifyListUrl).buildUpon()
                .appendPath("edit.json")
                .toString()

        private fun addUrl() = Uri.parse(baseModifyListUrl).buildUpon()
                .appendPath("add.json")
                .toString()

        private fun listEntryUrl(mediaId: Int) = Uri.parse(baseModifyListUrl).buildUpon()
                .appendPath(mediaId.toString())
                .appendPath("edit")
                .toString()

        private fun loginPostBody(username: String, password: String, csrf: String): RequestBody {
            return FormBody.Builder()
                    .add("user_name", username)
                    .add("password", password)
                    .add("cookie", "1")
                    .add("sublogin", "Login")
                    .add("submit", "1")
                    .add(CSRF, csrf)
                    .build()
        }

        private fun exportPostBody(): RequestBody {
            return FormBody.Builder()
                    .add("type", "2")
                    .add("subexport", "Export My List")
                    .build()
        }

        private fun mangaPostPayload(track: Track): RequestBody {
            val body = JSONObject()
                    .put("manga_id", track.media_id)
                    .put("status", track.status)
                    .put("score", track.score)
                    .put("num_read_chapters", track.last_chapter_read)

            return body.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        }

        private fun Element.searchTitle() = select("strong").text()!!

        private fun Element.searchTotalChapters() = if (select(TD)[4].text() == "-") 0 else select(TD)[4].text().toInt()

        private fun Element.searchCoverUrl() = select("img")
                .attr("data-src")
                .split("\\?")[0]
                .replace("/r/50x70/", "/")

        private fun Element.searchMediaId() = select("div.picSurround")
                .select("a").attr("id")
                .replace("sarea", "")
                .toInt()

        private fun Element.searchSummary() = select("div.pt4")
                .first()
                .ownText()!!

        private fun Element.searchPublishingStatus() = if (select(TD).last().text() == "-") "Publishing" else "Finished"

        private fun Element.searchPublishingType() = select(TD)[2].text()!!

        private fun Element.searchStartDate() = select(TD)[6].text()!!

        private fun getStatus(status: String) = when (status) {
            "Reading" -> 1
            "Completed" -> 2
            "On-Hold" -> 3
            "Dropped" -> 4
            "Plan to Read" -> 6
            else -> 1
        }
    }
}
