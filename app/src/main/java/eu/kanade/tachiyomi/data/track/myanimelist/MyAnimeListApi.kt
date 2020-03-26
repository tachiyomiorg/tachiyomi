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
            authClient.newCall(GET(url = editPageUrl(track.media_id)))
                    .asObservableSuccess()
                    .map { response ->
                        var libTrack: MyAnimeListEditData?
                        response.use {
                            if (response.priorResponse?.isRedirect != true) {
                                val tables = Jsoup.parse(it.consumeBody())
                                        .select("form#main-form table")

                                val entry_id = tables[0].select("input[name=entry_id]").`val`() // Always 0
                                val manga_id = tables[0].select("#manga_id").`val`()
                                val status = tables[0].select("#add_manga_status > option[selected]").`val`()
                                val num_read_volumes = tables[0].select("#add_manga_num_read_volumes").`val`()
                                val last_completed_vol = tables[0].select("input[name=last_completed_vol]").`val`() // Always Empty
                                val num_read_chapters = tables[0].select("#add_manga_num_read_chapters").`val`()
                                val score = tables[0].select("#add_manga_score > option[selected]").`val`()
                                val start_date_month = tables[0].select("#add_manga_start_date_month > option[selected]").`val`()
                                val start_date_day = tables[0].select("#add_manga_start_date_day > option[selected]").`val`()
                                val start_date_year = tables[0].select("#add_manga_start_date_year > option[selected]").`val`()
                                val finish_date_month = tables[0].select("#add_manga_finish_date_month > option[selected]").`val`()
                                val finish_date_day = tables[0].select("#add_manga_finish_date_day > option[selected]").`val`()
                                val finish_date_year = tables[0].select("#add_manga_finish_date_year > option[selected]").`val`()
                                val tags = tables[1].select("#add_manga_tags").`val`()
                                val priority = tables[1].select("#add_manga_priority > option[selected]").`val`()
                                val storage_type = tables[1].select("#add_manga_storage_type > option[selected]").`val`()
                                val num_retail_volumes = tables[1].select("#add_manga_num_retail_volumes").`val`()
                                val num_read_times = tables[1].select("#add_manga_num_read_times").`val`()
                                val reread_value = tables[1].select("#add_manga_reread_value > option[selected]").`val`()
                                val comments = tables[1].select("#add_manga_comments").`val`()
                                val is_asked_to_discuss = tables[1].select("#add_manga_is_asked_to_discuss > option[selected]").`val`()
                                val sns_post_type = tables[1].select("#add_manga_sns_post_type > option[selected]").`val`()

                                libTrack = MyAnimeListEditData(
                                        entry_id,
                                        manga_id,
                                        status,
                                        num_read_volumes,
                                        last_completed_vol,
                                        num_read_chapters,
                                        score,
                                        start_date_month, start_date_day, start_date_year,
                                        finish_date_month, finish_date_day, finish_date_year,
                                        tags,
                                        priority,
                                        storage_type,
                                        num_retail_volumes,
                                        num_read_times,
                                        reread_value,
                                        comments,
                                        is_asked_to_discuss,
                                        sns_post_type
                                )

                                libTrack!!.copyPersonalFrom(track)

                                authClient.newCall(POST(url = editPageUrl(track.media_id), body = mangaEditPostBody(libTrack!!))).execute()
                            }
                        }

                        track
                    }
        }
    }

    fun findLibManga(track: Track): Observable<Track?> {
        return authClient.newCall(GET(url = editPageUrl(track.media_id)))
                .asObservable()
                .map { response ->
                    var libTrack: Track? = null
                    response.use {
                        if (it.priorResponse?.isRedirect != true) {
                            val trackForm = Jsoup.parse(it.consumeBody())

                            libTrack = Track.create(TrackManager.MYANIMELIST).apply {
                                last_chapter_read = trackForm.select("#add_manga_num_read_chapters").`val`().toInt()
                                total_chapters = trackForm.select("#totalChap").text().toInt()
                                status = trackForm.select("#add_manga_status > option[selected]").`val`().toInt()
                                score = trackForm.select("#add_manga_score > option[selected]").`val`().toFloatOrNull()
                                        ?: 0f
                                started_reading_date = trackForm.getEditStartDate()
                                finished_reading_date = trackForm.getEditFinishDate()
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

        private fun editPageUrl(mediaId: Int) = Uri.parse(baseModifyListUrl).buildUpon()
                .appendPath(mediaId.toString())
                .appendPath("edit")
                .toString()

        private fun addUrl() = Uri.parse(baseModifyListUrl).buildUpon()
                .appendPath("add.json")
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

        private fun mangaEditPostBody(track: MyAnimeListEditData): RequestBody {
            return FormBody.Builder()
                    .add("entry_id", track.entry_id)
                    .add("manga_id", track.manga_id)
                    .add("add_manga[status]", track.status)
                    .add("add_manga[num_read_volumes]", track.num_read_volumes)
                    .add("last_completed_vol", track.last_completed_vol)
                    .add("add_manga[num_read_chapters]", track.num_read_chapters)
                    .add("add_manga[score]", track.score)
                    .add("add_manga[start_date][month]", track.start_date_month)
                    .add("add_manga[start_date][day]", track.start_date_day)
                    .add("add_manga[start_date][year]", track.start_date_year)
                    .add("add_manga[finish_date][month]", track.finish_date_month)
                    .add("add_manga[finish_date][day]", track.finish_date_day)
                    .add("add_manga[finish_date][year]", track.finish_date_year)
                    .add("add_manga[tags]", track.tags)
                    .add("add_manga[priority]", track.priority)
                    .add("add_manga[storage_type]", track.storage_type)
                    .add("add_manga[num_retail_volumes]", track.num_retail_volumes)
                    .add("add_manga[num_read_times]", track.num_read_chapters)
                    .add("add_manga[reread_value]", track.reread_value)
                    .add("add_manga[comments]", track.comments)
                    .add("add_manga[is_asked_to_discuss]", track.is_asked_to_discuss)
                    .add("add_manga[sns_post_type]", track.sns_post_type)
                    .add("submitIt", track.submitIt)
                    .build()
        }

        private fun Element.getEditStartDate(): Calendar? {
            val month = select("#add_manga_start_date_month > option[selected]").`val`().toIntOrNull()
            val day = select("#add_manga_start_date_day > option[selected]").`val`().toIntOrNull()
            val year = select("#add_manga_start_date_year > option[selected]").`val`().toIntOrNull()
            if (year == null || month == null || day == null)
                return null

            return GregorianCalendar(year, month - 1, day)
        }

        private fun Element.getEditFinishDate(): Calendar? {
            val month = select("#add_manga_finish_date_month > option[selected]").`val`().toIntOrNull()
            val day = select("#add_manga_finish_date_day > option[selected]").`val`().toIntOrNull()
            val year = select("#add_manga_finish_date_year > option[selected]").`val`().toIntOrNull()
            if (year == null || month == null || day == null)
                return null

            return GregorianCalendar(year, month - 1, day)
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

    private class MyAnimeListEditData(
            // entry_id: 0
        var entry_id: String,

            // manga_id: 2
        var manga_id: String,

            // add_manga[status]: 1
        var status: String,

            // add_manga[num_read_volumes]: 0
        var num_read_volumes: String,

            // last_completed_vol:
        var last_completed_vol: String,

            // add_manga[num_read_chapters]: 0
        var num_read_chapters: String,

            // add_manga[score]:
        var score: String,

            // add_manga[start_date][month]:
        var start_date_month: String, // [1-12]

            // add_manga[start_date][day]:
        var start_date_day: String,

            // add_manga[start_date][year]:
        var start_date_year: String,

            // add_manga[finish_date][month]:
        var finish_date_month: String, // [1-12]

            // add_manga[finish_date][day]:
        var finish_date_day: String,

            // add_manga[finish_date][year]:
        var finish_date_year: String,

            // add_manga[tags]:
        var tags: String,

            // add_manga[priority]: 0
        var priority: String,

            // add_manga[storage_type]:
        var storage_type: String,

            // add_manga[num_retail_volumes]: 0
        var num_retail_volumes: String,

            // add_manga[num_read_times]: 0
        var num_read_times: String,

            // add_manga[reread_value]:
        var reread_value: String,

            // add_manga[comments]:
        var comments: String,

            // add_manga[is_asked_to_discuss]: 0
        var is_asked_to_discuss: String,

            // add_manga[sns_post_type]: 0
        var sns_post_type: String,

            // submitIt: 0
        val submitIt: String = "0"
    ) {
        fun copyPersonalFrom(track: Track) {
            num_read_chapters = track.last_chapter_read.toString()
            val numScore = track.score.toInt()
            if (numScore in 1..9)
                score = numScore.toString()
            status = track.status.toString()
            if (track.started_reading_date != null) {
                start_date_month = (track.started_reading_date!!.get(Calendar.MONTH) + 1).toString()
                start_date_day = track.started_reading_date!!.get(Calendar.DAY_OF_MONTH).toString()
                start_date_year = track.started_reading_date!!.get(Calendar.YEAR).toString()
            }
            if (track.finished_reading_date != null) {
                finish_date_month = (track.finished_reading_date!!.get(Calendar.MONTH) + 1).toString()
                finish_date_day = track.finished_reading_date!!.get(Calendar.DAY_OF_MONTH).toString()
                finish_date_year = track.finished_reading_date!!.get(Calendar.YEAR).toString()
            }
        }
    }
}
