package eu.kanade.tachiyomi.data.track.myanimelist

import android.net.Uri
import android.util.Xml
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
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.xmlpull.v1.XmlSerializer
import rx.Observable
import java.io.StringWriter

class MyanimelistApi(private val client: OkHttpClient, username: String, password: String) {

    private var headers = createHeaders(username, password)

    fun addLibManga(track: Track): Observable<Track> {
        return Observable.defer {
            client.newCall(POST(getAddUrl(track), headers, getMangaPostPayload(track)))
                    .asObservableSuccess()
                    .map { track }
        }
    }

    fun updateLibManga(track: Track): Observable<Track> {
        return Observable.defer {
            client.newCall(POST(getUpdateUrl(track), headers, getMangaPostPayload(track)))
                    .asObservableSuccess()
                    .map { track }
        }
    }

    fun search(query: String, username: String): Observable<List<TrackSearch>> {
        return if (query.startsWith(PREFIX_MY)) {
            val realQuery = query.substring(PREFIX_MY.length).toLowerCase().trim()
            getList(username)
                    .flatMap { Observable.from(it) }
                    .filter { realQuery in it.title.toLowerCase() }
                    .toList()
        } else {
            client.newCall(GET(getSearchUrl(query), headers))
                    .asObservable()
                    .map { Jsoup.parse(Parser.unescapeEntities(it.body()!!.string(), false), "", Parser.xmlParser()) }
                    .flatMap { Observable.from(it.select("entry")) }
                    .filter { it.select("type").text() != "Novel" }
                    .map {
                        TrackSearch.create(TrackManager.MYANIMELIST).apply {
                            title = it.selectText("title")!!
                            media_id = it.selectInt("id")
                            total_chapters = it.selectInt("chapters")
                            summary = it.selectText("synopsis")!!
                            cover_url = it.selectText("image")!!
                            tracking_url = MyanimelistApi.mangaUrl(media_id)
                            publishing_status = it.selectText("status")!!
                            publishing_type = it.selectText("type")!!
                            start_date = it.selectText("start_date")!!
                        }
                    }
                    .toList()
        }
    }

    fun getList(username: String): Observable<List<TrackSearch>> {
        return client
                .newCall(GET(getListUrl(username), headers))
                .asObservable()
                .map { Jsoup.parse(Parser.unescapeEntities(it.body()!!.string(), false), "", Parser.xmlParser()) }
                .flatMap { Observable.from(it.select("manga")) }
                .map {
                    TrackSearch.create(TrackManager.MYANIMELIST).apply {
                        title = it.selectText("series_title")!!
                        media_id = it.selectInt("series_mangadb_id")
                        last_chapter_read = it.selectInt("my_read_chapters")
                        status = it.selectInt("my_status")
                        score = it.selectInt("my_score").toFloat()
                        total_chapters = it.selectInt("series_chapters")
                        cover_url = it.selectText("series_image")!!
                        tracking_url = MyanimelistApi.mangaUrl(media_id)
                    }
                }
                .toList()
    }

    fun findLibManga(track: Track, username: String): Observable<Track?> {
        return getList(username)
                .map { list -> list.find { it.media_id == track.media_id } }
    }

    fun getLibManga(track: Track, username: String): Observable<Track> {
        return findLibManga(track, username)
                .map { it ?: throw Exception("Could not find manga") }
    }

    fun login(username: String, password: String): Observable<Response> {
        headers = createHeaders(username, password)
        return client.newCall(GET(getLoginUrl(), headers))
                .asObservable()
                .doOnNext { response ->
                    response.close()
                    if (response.code() != 200) throw Exception("Login error")
                }
    }

    private fun getMangaPostPayload(track: Track): RequestBody {
        val data = xml {
            element(ENTRY_TAG) {
                if (track.last_chapter_read != 0) {
                    text(CHAPTER_TAG, track.last_chapter_read.toString())
                }
                text(STATUS_TAG, track.status.toString())
                text(SCORE_TAG, track.score.toString())
            }
        }

        return FormBody.Builder()
                .add("data", data)
                .build()
    }

    private inline fun xml(block: XmlSerializer.() -> Unit): String {
        val x = Xml.newSerializer()
        val writer = StringWriter()

        with(x) {
            setOutput(writer)
            startDocument("UTF-8", false)
            block()
            endDocument()
        }

        return writer.toString()
    }

    private inline fun XmlSerializer.element(tag: String, block: XmlSerializer.() -> Unit) {
        startTag("", tag)
        block()
        endTag("", tag)
    }

    private fun XmlSerializer.text(tag: String, body: String) {
        startTag("", tag)
        text(body)
        endTag("", tag)
    }

    fun getLoginUrl() = Uri.parse(baseUrl).buildUpon()
            .appendEncodedPath("api/account/verify_credentials.xml")
            .toString()

    fun getSearchUrl(query: String) = Uri.parse(baseUrl).buildUpon()
            .appendEncodedPath("api/manga/search.xml")
            .appendQueryParameter("q", query)
            .toString()

    fun getListUrl(username: String) = Uri.parse(baseUrl).buildUpon()
            .appendPath("malappinfo.php")
            .appendQueryParameter("u", username)
            .appendQueryParameter("status", "all")
            .appendQueryParameter("type", "manga")
            .toString()

    fun getUpdateUrl(track: Track) = Uri.parse(baseUrl).buildUpon()
            .appendEncodedPath("api/mangalist/update")
            .appendPath("${track.media_id}.xml")
            .toString()

    fun getAddUrl(track: Track) = Uri.parse(baseUrl).buildUpon()
            .appendEncodedPath("api/mangalist/add")
            .appendPath("${track.media_id}.xml")
            .toString()

    fun createHeaders(username: String, password: String): Headers {
        return Headers.Builder()
                .add("Authorization", Credentials.basic(username, password))
                .add("User-Agent", "api-indiv-9F93C52A963974CF674325391990191C")
                .build()
    }

    companion object {
        const val baseUrl = "https://myanimelist.net"
        const val baseMangaUrl = baseUrl + "/manga/"

        fun mangaUrl(remoteId: Int): String {
            return baseMangaUrl + remoteId
        }

        private val ENTRY_TAG = "entry"
        private val CHAPTER_TAG = "chapter"
        private val SCORE_TAG = "score"
        private val STATUS_TAG = "status"

        const val PREFIX_MY = "my:"
    }
}