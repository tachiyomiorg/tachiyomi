package eu.kanade.tachiyomi.data.mangasync.myanimelist

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Xml
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.mangasync.MangaSyncService
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.POST
import eu.kanade.tachiyomi.data.network.asObservable
import eu.kanade.tachiyomi.util.selectInt
import eu.kanade.tachiyomi.util.selectText
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.RequestBody
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlSerializer
import rx.Completable
import rx.Observable
import java.io.StringWriter

class MyAnimeList(private val context: Context, id: Int) : MangaSyncService(context, id) {

    private lateinit var headers: Headers

    companion object {
        const val BASE_URL = "https://myanimelist.net"

        private val ENTRY_TAG = "entry"
        private val CHAPTER_TAG = "chapter"
        private val SCORE_TAG = "score"
        private val STATUS_TAG = "status"

        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 6

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0

        const val PREFIX_MY = "my:"
    }

    init {
        val username = getUsername()
        val password = getPassword()

        if (!username.isEmpty() && !password.isEmpty()) {
            createHeaders(username, password)
        }
    }

    override val name: String
        get() = "MyAnimeList"

    override fun getLogo() = R.drawable.mal

    override fun getLogoColor() = Color.rgb(46, 81, 162)

    override fun maxScore() = 10

    override fun formatScore(manga: MangaSync): String {
        return manga.score.toInt().toString()
    }

    fun getLoginUrl() = Uri.parse(BASE_URL).buildUpon()
            .appendEncodedPath("api/account/verify_credentials.xml")
            .toString()

    fun getSearchUrl(query: String) = Uri.parse(BASE_URL).buildUpon()
            .appendEncodedPath("api/manga/search.xml")
            .appendQueryParameter("q", query)
            .toString()

    fun getListUrl(username: String) = Uri.parse(BASE_URL).buildUpon()
            .appendPath("malappinfo.php")
            .appendQueryParameter("u", username)
            .appendQueryParameter("status", "all")
            .appendQueryParameter("type", "manga")
            .toString()

    fun getUpdateUrl(manga: MangaSync) = Uri.parse(BASE_URL).buildUpon()
            .appendEncodedPath("api/mangalist/update")
            .appendPath("${manga.remote_id}.xml")
            .toString()

    fun getAddUrl(manga: MangaSync) = Uri.parse(BASE_URL).buildUpon()
            .appendEncodedPath("api/mangalist/add")
            .appendPath("${manga.remote_id}.xml")
            .toString()

    override fun login(username: String, password: String): Completable {
        createHeaders(username, password)
        return client.newCall(GET(getLoginUrl(), headers))
                .asObservable()
                .doOnNext { it.close() }
                .doOnNext { if (it.code() != 200) throw Exception("Login error") }
                .toCompletable()
    }

    override fun search(query: String): Observable<List<MangaSync>> {
        return if (query.startsWith(PREFIX_MY)) {
            val realQuery = query.substring(PREFIX_MY.length).toLowerCase().trim()
            getList()
                    .flatMap { Observable.from(it) }
                    .filter { realQuery in it.title.toLowerCase() }
                    .toList()
        } else {
            client.newCall(GET(getSearchUrl(query), headers))
                    .asObservable()
                    .map { Jsoup.parse(it.body().string()) }
                    .flatMap { Observable.from(it.select("entry")) }
                    .filter { it.select("type").text() != "Novel" }
                    .map {
                        MangaSync.create(id).apply {
                            title = it.selectText("title")!!
                            remote_id = it.selectInt("id")
                            total_chapters = it.selectInt("chapters")
                        }
                    }
                    .toList()
        }
    }

    override fun refresh(manga: MangaSync): Observable<MangaSync> {
        return getList()
                .map { myList ->
                    val myManga = myList.find { it.remote_id == manga.remote_id }
                    if (myManga != null) {
                        manga.copyPersonalFrom(myManga)
                        manga.total_chapters = myManga.total_chapters
                        manga
                    } else {
                        throw Exception("Could not find manga")
                    }
                }
    }

    // MAL doesn't support score with decimals
    fun getList(): Observable<List<MangaSync>> {
        return networkService.forceCacheClient
                .newCall(GET(getListUrl(getUsername()), headers))
                .asObservable()
                .map { Jsoup.parse(it.body().string()) }
                .flatMap { Observable.from(it.select("manga")) }
                .map {
                    MangaSync.create(id).apply {
                        title = it.selectText("series_title")!!
                        remote_id = it.selectInt("series_mangadb_id")
                        last_chapter_read = it.selectInt("my_read_chapters")
                        status = it.selectInt("my_status")
                        score = it.selectInt("my_score").toFloat()
                        total_chapters = it.selectInt("series_chapters")
                    }
                }
                .toList()
    }

    override fun update(manga: MangaSync): Observable<MangaSync> {
        return Observable.defer {
            if (manga.total_chapters != 0 && manga.last_chapter_read == manga.total_chapters) {
                manga.status = COMPLETED
            }
            client.newCall(POST(getUpdateUrl(manga), headers, getMangaPostPayload(manga)))
                    .asObservable()
                    .doOnNext { it.close() }
                    .doOnNext { if (!it.isSuccessful) throw Exception("Could not update manga") }
                    .map { manga }
        }

    }

    override fun add(manga: MangaSync): Observable<MangaSync> {
        return Observable.defer {
            client.newCall(POST(getAddUrl(manga), headers, getMangaPostPayload(manga)))
                    .asObservable()
                    .doOnNext { it.close() }
                    .doOnNext { if (!it.isSuccessful) throw Exception("Could not add manga") }
                    .map { manga }
        }
    }

    private fun getMangaPostPayload(manga: MangaSync): RequestBody {
        val xml = Xml.newSerializer()
        val writer = StringWriter()

        with(xml) {
            setOutput(writer)
            startDocument("UTF-8", false)
            startTag("", ENTRY_TAG)

            // Last chapter read
            if (manga.last_chapter_read != 0) {
                inTag(CHAPTER_TAG, manga.last_chapter_read.toString())
            }
            // Manga status in the list
            inTag(STATUS_TAG, manga.status.toString())

            // Manga score
            inTag(SCORE_TAG, manga.score.toString())

            endTag("", ENTRY_TAG)
            endDocument()
        }

        val form = FormBody.Builder()
        form.add("data", writer.toString())
        return form.build()
    }

    fun XmlSerializer.inTag(tag: String, body: String, namespace: String = "") {
        startTag(namespace, tag)
        text(body)
        endTag(namespace, tag)
    }

    override fun bind(manga: MangaSync): Observable<MangaSync> {
        return getList()
                .flatMap { userlist ->
                    manga.sync_id = id
                    val mangaFromList = userlist.find { it.remote_id == manga.remote_id }
                    if (mangaFromList != null) {
                        manga.copyPersonalFrom(mangaFromList)
                        update(manga)
                    } else {
                        // Set default fields if it's not found in the list
                        manga.score = DEFAULT_SCORE.toFloat()
                        manga.status = DEFAULT_STATUS
                        add(manga)
                    }
                }
    }

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(R.string.reading)
            COMPLETED -> getString(R.string.completed)
            ON_HOLD -> getString(R.string.on_hold)
            DROPPED -> getString(R.string.dropped)
            PLAN_TO_READ -> getString(R.string.plan_to_read)
            else -> ""
        }
    }

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

    fun createHeaders(username: String, password: String) {
        val builder = Headers.Builder()
        builder.add("Authorization", Credentials.basic(username, password))
        builder.add("User-Agent", "api-indiv-9F93C52A963974CF674325391990191C")
        headers = builder.build()
    }

}
