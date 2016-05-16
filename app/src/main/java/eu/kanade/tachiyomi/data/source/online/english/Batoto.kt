package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context
import android.net.Uri
import android.text.Html
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.get
import eu.kanade.tachiyomi.data.network.post
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.base.ParsedOnlineSource
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.net.URI
import java.net.URISyntaxException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Batoto(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Batoto"

    override val baseUrl = "http://bato.to"

    override val lang: Language get() = EN

    private val datePattern = Pattern.compile("(\\d+|A|An)\\s+(.*?)s? ago.*")

    private val dateFields = HashMap<String, Int>().apply {
        put("second", Calendar.SECOND)
        put("minute", Calendar.MINUTE)
        put("hour", Calendar.HOUR)
        put("day", Calendar.DATE)
        put("week", Calendar.WEEK_OF_YEAR)
        put("month", Calendar.MONTH)
        put("year", Calendar.YEAR)
    }

    private val staffNotice = Pattern.compile("=+Batoto Staff Notice=+([^=]+)==+", Pattern.CASE_INSENSITIVE)

    override fun headersBuilder() = super.headersBuilder()
            .add("Cookie", "lang_option=English")
            .add("Referer", "http://bato.to/reader")

    override fun popularMangaInitialUrl() = "$baseUrl/search_ajax?order_cond=views&order=desc&p=1"

    override fun searchMangaInitialUrl(query: String) = "$baseUrl/search_ajax?name=${Uri.encode(query)}&p=1"

    override fun mangaDetailsRequest(manga: Manga): Request {
        val mangaId = manga.url.substringAfterLast("r")
        return get("$baseUrl/comic_pop?id=$mangaId", headers)
    }

    override fun pageListRequest(chapter: Chapter): Request {
        val id = chapter.url.substringAfterLast("#")
        return get("$baseUrl/areader?id=$id&p=1", headers)
    }

    override fun imageUrlRequest(page: Page): Request {
        val pageUrl = page.url
        val start = pageUrl.indexOf("#") + 1
        val end = pageUrl.indexOf("_", start)
        val id = pageUrl.substring(start, end)
        return get("$baseUrl/areader?id=$id&p=${pageUrl.substring(end+1)}", headers)
    }

    override fun popularMangaParse(response: Response, page: MangasPage) {
        val document = Jsoup.parse(response.body().string())
        for (element in document.select(popularMangaSelector())) {
            Manga().apply {
                source = this@Batoto.id
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(popularMangaNextPageSelector()).first()?.let {
            "$baseUrl/search_ajax?order_cond=views&order=desc&p=${page.page + 1}"
        }
    }

    override fun popularMangaSelector() = "tr:not([id]):not([class])"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("a[href^=http://bato.to]").first().let {
            manga.setUrl(it.attr("href"))
            manga.title = it.text().trim()
        }
    }

    override fun popularMangaNextPageSelector() = "#show_more_row"

    override fun searchMangaParse(response: Response, page: MangasPage, query: String) {
        val document = Jsoup.parse(response.body().string())
        for (element in document.select(searchMangaSelector())) {
            Manga().apply {
                source = this@Batoto.id
                searchMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(searchMangaNextPageSelector()).first()?.let {
            "$baseUrl/search_ajax?name=${Uri.encode(query)}&p=${page.page + 1}"
        }
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val tbody = document.select("tbody").first()
        val artistElement = tbody.select("tr:contains(Author/Artist:)").first()

        manga.author = artistElement.select("td:eq(1)").first()?.text()
        manga.artist = artistElement.select("td:eq(2)").first()?.text() ?: manga.author
        manga.description = tbody.select("tr:contains(Description:) > td:eq(1)").first()?.text()
        manga.thumbnail_url = document.select("img[src^=http://img.bato.to/forums/uploads/]").first()?.attr("src")
        manga.status = parseStatus(document.select("tr:contains(Status:) > td:eq(1)").first()?.text())
        manga.genre = tbody.select("tr:contains(Genres:) img").map { it.attr("alt") }.joinToString(", ")
    }

    private fun parseStatus(status: String?) = when (status) {
        "Ongoing" -> Manga.ONGOING
        "Complete" -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListParse(response: Response, chapters: MutableList<Chapter>) {
        val body = response.body().string()
        val matcher = staffNotice.matcher(body)
        if (matcher.find()) {
            val notice = Html.fromHtml(matcher.group(1)).toString().trim()
            throw RuntimeException(notice)
        }

        val document = Jsoup.parse(body)

        for (element in document.select(chapterListSelector())) {
            Chapter.create().apply {
                chapterFromElement(element, this)
                chapters.add(this)
            }
        }
    }

    override fun chapterListSelector() = "tr.row.lang_English.chapter_row"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a[href^=http://bato.to/reader").first()

        chapter.setUrl(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td").getOrNull(4)?.let {
            parseDateFromElement(it)
        } ?: 0
    }

    private fun parseDateFromElement(dateElement: Element): Long {
        val dateAsString = dateElement.text()

        val date: Date
        try {
            date = SimpleDateFormat("dd MMMMM yyyy - hh:mm a", Locale.ENGLISH).parse(dateAsString)
        } catch (e: ParseException) {
            val m = datePattern.matcher(dateAsString)

            if (m.matches()) {
                val number = m.group(1)
                val amount = if (number.contains("A")) 1 else Integer.parseInt(m.group(1))
                val unit = m.group(2)

                date = Calendar.getInstance().apply {
                    add(dateFields[unit]!!, -amount)
                }.time
            } else {
                return 0
            }
        }

        return date.time
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        val selectElement = document.select("#page_select").first()
        if (selectElement != null) {
            for ((i, element) in selectElement.select("option").withIndex()) {
                pages.add(Page(i, element.attr("value")))
            }
        } else {
            // For webtoons in one page
            for ((i, element) in document.select("div > img").withIndex()) {
                pages.add(Page(i, "", element.attr("src")))
            }
        }
    }

    override fun imageUrlParse(document: Document): String {
        return document.select("#comic_page").first().attr("src")
    }

    override fun login(username: String, password: String) =
        network.requestBody(get("$baseUrl/forums/index.php?app=core&module=global&section=login", headers))
                .flatMap { doLogin(it, username, password) }
                .map { isAuthenticationSuccessful(it) }

    private fun doLogin(response: String, username: String, password: String): Observable<Response> {
        val doc = Jsoup.parse(response)
        val form = doc.select("#login").first()
        val url = form.attr("action")
        val authKey = form.select("input[name=auth_key]").first()

        val payload = FormBody.Builder().apply {
            add(authKey.attr("name"), authKey.attr("value"))
            add("ips_username", username)
            add("ips_password", password)
            add("invisible", "1")
            add("rememberMe", "1")
        }.build()

        return network.request(post(url, headers, payload))
    }

    override fun isLoginRequired() = true

    override fun isAuthenticationSuccessful(response: Response) =
        response.priorResponse() != null && response.priorResponse().code() == 302

    override fun isLogged(): Boolean {
        try {
            return network.cookies.get(URI(baseUrl)).find { it.name() == "pass_hash" } != null
        } catch (e: URISyntaxException) {
            // Ignore
        }
        return false
    }

    override fun fetchChapterList(manga: Manga): Observable<List<Chapter>> {
        if (!isLogged()) {
            val username = preferences.sourceUsername(this)
            val password = preferences.sourcePassword(this)

            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                return Observable.error(Exception("User not logged"))
            } else {
                return login(username, password).flatMap { super.fetchChapterList(manga) }
            }

        } else {
            return super.fetchChapterList(manga)
        }
    }

}