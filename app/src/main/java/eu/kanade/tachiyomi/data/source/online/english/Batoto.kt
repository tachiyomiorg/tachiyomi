package eu.kanade.tachiyomi.data.source.online.english

import android.net.Uri
import android.text.Html
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.POST
import eu.kanade.tachiyomi.data.network.asObservable
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.LoginSource
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.selectText
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.net.URI
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Batoto(override val id: Int) : ParsedOnlineSource(), LoginSource {

    override val name = "Batoto"

    override val baseUrl = "http://bato.to"

    override val lang = "en"

    override val supportsLatest = true

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

    private val pageHeaders = super.headersBuilder()
            .add("Referer", "http://bato.to/reader")
            .build()

    override fun popularMangaInitialUrl() = "$baseUrl/search_ajax?order_cond=views&order=desc&p=1"

    override fun latestUpdatesInitialUrl() = "$baseUrl/search_ajax?order_cond=update&order=desc&p=1"

    override fun popularMangaParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(popularMangaSelector())) {
            Manga.create(id).apply {
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(popularMangaNextPageSelector()).first()?.let {
            "$baseUrl/search_ajax?order_cond=views&order=desc&p=${page.page + 1}"
        }
    }

    override fun latestUpdatesParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(latestUpdatesSelector())) {
            Manga.create(id).apply {
                latestUpdatesFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(latestUpdatesNextPageSelector()).first()?.let {
            "$baseUrl/search_ajax?order_cond=update&order=desc&p=${page.page + 1}"
        }
    }

    override fun popularMangaSelector() = "tr:has(a)"

    override fun latestUpdatesSelector() = "tr:has(a)"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("a[href^=http://bato.to]").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text().trim()
        }
    }

    override fun latestUpdatesFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun popularMangaNextPageSelector() = "#show_more_row"

    override fun latestUpdatesNextPageSelector() = "#show_more_row"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) = "$baseUrl/search_ajax?name=${Uri.encode(query)}&order_cond=views&order=desc&p=1${getFilterParams(filters)}"

    private fun getFilterParams(filters: List<Filter>): String {
        var genres = ""
        var completed = ""
        for (filter in filters) {
            if (filter.equals(completedFilter)) completed = "&completed=c"
            else genres += ";i" + filter.id
        }
        return if (genres.isEmpty()) completed else "&genres=$genres&genre_cond=and$completed"
    }

    override fun searchMangaRequest(page: MangasPage, query: String, filters: List<Filter>): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query, filters)
        }
        return GET(page.url, headers)
    }

    override fun searchMangaParse(response: Response, page: MangasPage, query: String, filters: List<Filter>) {
        val document = response.asJsoup()
        for (element in document.select(searchMangaSelector())) {
            Manga.create(id).apply {
                searchMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(searchMangaNextPageSelector()).first()?.let {
            "$baseUrl/search_ajax?name=${Uri.encode(query)}&order_cond=views&order=desc&p=${page.page + 1}${getFilterParams(filters)}"
        }
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsRequest(manga: Manga): Request {
        val mangaId = manga.url.substringAfterLast("r")
        return GET("$baseUrl/comic_pop?id=$mangaId", headers)
    }

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val tbody = document.select("tbody").first()
        val artistElement = tbody.select("tr:contains(Author/Artist:)").first()

        manga.author = artistElement.selectText("td:eq(1)")
        manga.artist = artistElement.selectText("td:eq(2)") ?: manga.author
        manga.description = tbody.selectText("tr:contains(Description:) > td:eq(1)")
        manga.thumbnail_url = document.select("img[src^=http://img.bato.to/forums/uploads/]").first()?.attr("src")
        manga.status = parseStatus(document.selectText("tr:contains(Status:) > td:eq(1)"))
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
            throw Exception(notice)
        }

        val document = response.asJsoup(body)

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

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td").getOrNull(4)?.let {
            parseDateFromElement(it)
        } ?: 0
    }

    private fun parseDateFromElement(dateElement: Element): Long {
        val dateAsString = dateElement.text()

        var date: Date
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

    override fun pageListRequest(chapter: Chapter): Request {
        val id = chapter.url.substringAfterLast("#")
        return GET("$baseUrl/areader?id=$id&p=1", pageHeaders)
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        val selectElement = document.select("#page_select").first()
        if (selectElement != null) {
            for ((i, element) in selectElement.select("option").withIndex()) {
                pages.add(Page(i, element.attr("value")))
            }
            pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
        } else {
            // For webtoons in one page
            for ((i, element) in document.select("div > img").withIndex()) {
                pages.add(Page(i, "", element.attr("src")))
            }
        }
    }

    override fun imageUrlRequest(page: Page): Request {
        val pageUrl = page.url
        val start = pageUrl.indexOf("#") + 1
        val end = pageUrl.indexOf("_", start)
        val id = pageUrl.substring(start, end)
        return GET("$baseUrl/areader?id=$id&p=${pageUrl.substring(end + 1)}", pageHeaders)
    }

    override fun imageUrlParse(document: Document): String {
        return document.select("#comic_page").first().attr("src")
    }

    override fun login(username: String, password: String) =
            client.newCall(GET("$baseUrl/forums/index.php?app=core&module=global&section=login", headers))
                    .asObservable()
                    .flatMap { doLogin(it, username, password) }
                    .map { isAuthenticationSuccessful(it) }

    private fun doLogin(response: Response, username: String, password: String): Observable<Response> {
        val doc = response.asJsoup()
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

        return client.newCall(POST(url, headers, payload)).asObservable()
    }

    override fun isAuthenticationSuccessful(response: Response) =
            response.priorResponse() != null && response.priorResponse().code() == 302

    override fun isLogged(): Boolean {
        return network.cookies.get(URI(baseUrl)).any { it.name() == "pass_hash" }
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

    private val completedFilter = Filter("completed", "Completed")
    // [...document.querySelectorAll("#advanced_options div.genre_buttons")].map((el,i) => {
    //     const onClick=el.getAttribute('onclick');const id=onClick.substr(14,onClick.length-16);return `Filter("${id}", "${el.textContent.trim()}")`
    // }).join(',\n')
    // on https://bato.to/search
    override fun getFilterList(): List<Filter> = listOf(
            completedFilter,
            Filter("40", "4-Koma"),
            Filter("1", "Action"),
            Filter("2", "Adventure"),
            Filter("39", "Award Winning"),
            Filter("3", "Comedy"),
            Filter("41", "Cooking"),
            Filter("9", "Doujinshi"),
            Filter("10", "Drama"),
            Filter("12", "Ecchi"),
            Filter("13", "Fantasy"),
            Filter("15", "Gender Bender"),
            Filter("17", "Harem"),
            Filter("20", "Historical"),
            Filter("22", "Horror"),
            Filter("34", "Josei"),
            Filter("27", "Martial Arts"),
            Filter("30", "Mecha"),
            Filter("42", "Medical"),
            Filter("37", "Music"),
            Filter("4", "Mystery"),
            Filter("38", "Oneshot"),
            Filter("5", "Psychological"),
            Filter("6", "Romance"),
            Filter("7", "School Life"),
            Filter("8", "Sci-fi"),
            Filter("32", "Seinen"),
            Filter("35", "Shoujo"),
            Filter("16", "Shoujo Ai"),
            Filter("33", "Shounen"),
            Filter("19", "Shounen Ai"),
            Filter("21", "Slice of Life"),
            Filter("23", "Smut"),
            Filter("25", "Sports"),
            Filter("26", "Supernatural"),
            Filter("28", "Tragedy"),
            Filter("36", "Webtoon"),
            Filter("29", "Yaoi"),
            Filter("31", "Yuri")
    )

}