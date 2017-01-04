package eu.kanade.tachiyomi.data.source.online.english

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
import okhttp3.HttpUrl
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

    override fun searchMangaInitialUrl(query: String, filters: List<Filter<*>>) = searchMangaUrl(query, filters, 1)

    private fun searchMangaUrl(query: String, filterStates: List<Filter<*>>, page: Int): String {
        val url = HttpUrl.parse("$baseUrl/search_ajax").newBuilder()
        if (!query.isEmpty()) url.addQueryParameter("name", query).addQueryParameter("name_cond", "c")
        var genres = ""
        for (filter in if (filterStates.isEmpty()) filters else filterStates) {
            when (filter) {
                is Status -> if (filter.state != Filter.TriState.STATE_IGNORE) {
                    url.addQueryParameter("completed", if (filter.state == Filter.TriState.STATE_EXCLUDE) "i" else "c")
                }
                is Genre -> if (filter.state != Filter.TriState.STATE_IGNORE) {
                    genres += (if (filter.state == Filter.TriState.STATE_EXCLUDE) ";e" else ";i") + filter.id
                }
                is TextField -> {
                    if (!filter.state.isEmpty()) url.addQueryParameter(filter.key, filter.state)
                }
                is ListField -> {
                    val sel = filter.values[filter.state].value
                    if (!sel.isEmpty()) url.addQueryParameter(filter.key, sel)
                }
                is Flag -> {
                    val sel = if (filter.state) filter.valTrue else filter.valFalse
                    if (!sel.isEmpty()) url.addQueryParameter(filter.key, sel)
                }
            }
        }
        if (!genres.isEmpty()) url.addQueryParameter("genres", genres)
        url.addQueryParameter("p", page.toString())
        return url.toString()
    }

    override fun searchMangaRequest(page: MangasPage, query: String, filters: List<Filter<*>>): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query, filters)
        }
        return GET(page.url, headers)
    }

    override fun searchMangaParse(response: Response, page: MangasPage, query: String, filters: List<Filter<*>>) {
        val document = response.asJsoup()
        for (element in document.select(searchMangaSelector())) {
            Manga.create(id).apply {
                searchMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(searchMangaNextPageSelector()).first()?.let {
            searchMangaUrl(query, filters, page.page + 1)
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

    private data class ListValue(val name: String, val value: String) {
        override fun toString(): String = name
    }

    private class Status() : Filter.TriState("Completed")
    private class Genre(name: String, val id: Int) : Filter.TriState(name)
    private class TextField(name: String, val key: String) : Filter.Text(name)
    private class ListField(name: String, val key: String, values: Array<ListValue>, state: Int = 0) : Filter.List<ListValue>(name, values, state)
    private class Flag(name: String, val key: String, val valTrue: String, val valFalse: String) : Filter.CheckBox(name)

    // [...document.querySelectorAll("#advanced_options div.genre_buttons")].map((el,i) => {
    //     const onClick=el.getAttribute('onclick');const id=onClick.substr(14,onClick.length-16);return `Genre("${el.textContent.trim()}", ${id})`
    // }).join(',\n')
    // on https://bato.to/search
    override fun getFilterList(): List<Filter<*>> = listOf(
            TextField("Author", "artist_name"),
            ListField("Type", "type", arrayOf(ListValue("Any", ""), ListValue("Manga (Jp)", "jp"), ListValue("Manhwa (Kr)", "kr"), ListValue("Manhua (Cn)", "cn"), ListValue("Artbook", "ar"), ListValue("Other", "ot"))),
            Status(),
            Flag("Exclude mature", "mature", "m", ""),
            ListField("Order by", "order_cond", arrayOf(ListValue("Title", "title"), ListValue("Author", "author"), ListValue("Artist", "artist"), ListValue("Rating", "rating"), ListValue("Views", "views"), ListValue("Last Update", "update")), 4),
            Flag("Ascending order", "order", "asc", "desc"),
            Filter.Header("Genres"),
            ListField("Inclusion mode", "genre_cond", arrayOf(ListValue("And (all selected genres)", "and"), ListValue("Or (any selected genres) ", "or"))),
            Genre("4-Koma", 40),
            Genre("Action", 1),
            Genre("Adventure", 2),
            Genre("Award Winning", 39),
            Genre("Comedy", 3),
            Genre("Cooking", 41),
            Genre("Doujinshi", 9),
            Genre("Drama", 10),
            Genre("Ecchi", 12),
            Genre("Fantasy", 13),
            Genre("Gender Bender", 15),
            Genre("Harem", 17),
            Genre("Historical", 20),
            Genre("Horror", 22),
            Genre("Josei", 34),
            Genre("Martial Arts", 27),
            Genre("Mecha", 30),
            Genre("Medical", 42),
            Genre("Music", 37),
            Genre("Mystery", 4),
            Genre("Oneshot", 38),
            Genre("Psychological", 5),
            Genre("Romance", 6),
            Genre("School Life", 7),
            Genre("Sci-fi", 8),
            Genre("Seinen", 32),
            Genre("Shoujo", 35),
            Genre("Shoujo Ai", 16),
            Genre("Shounen", 33),
            Genre("Shounen Ai", 19),
            Genre("Slice of Life", 21),
            Genre("Smut", 23),
            Genre("Sports", 25),
            Genre("Supernatural", 26),
            Genre("Tragedy", 28),
            Genre("Webtoon", 36),
            Genre("Yaoi", 29),
            Genre("Yuri", 31),
            Genre("[no chapters]", 44)
    )

}