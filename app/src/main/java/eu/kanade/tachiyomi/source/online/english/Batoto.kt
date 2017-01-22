package eu.kanade.tachiyomi.source.online.english

import android.text.Html
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
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

class Batoto : ParsedHttpSource(), LoginSource {

    override val id: Long = 1

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

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/search_ajax?order_cond=views&order=desc&p=$page", headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/search_ajax?order_cond=update&order=desc&p=$page", headers)
    }

    override fun popularMangaSelector() = "tr:has(a)"

    override fun latestUpdatesSelector() = "tr:has(a)"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a[href^=http://bato.to]").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text().trim()
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun popularMangaNextPageSelector() = "#show_more_row"

    override fun latestUpdatesNextPageSelector() = "#show_more_row"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = HttpUrl.parse("$baseUrl/search_ajax").newBuilder()
        if (!query.isEmpty()) url.addQueryParameter("name", query).addQueryParameter("name_cond", "c")
        var genres = ""
        filters.forEach { filter ->
            when (filter) {
                is Status -> if (!filter.isIgnored()) {
                    url.addQueryParameter("completed", if (filter.isExcluded()) "i" else "c")
                }
                is GenreList -> {
                    filter.state.forEach { filter ->
                        when (filter) {
                            is Genre -> if (!filter.isIgnored()) {
                                genres += (if (filter.isExcluded()) ";e" else ";i") + filter.id
                            }
                            is SelectField -> {
                                val sel = filter.values[filter.state].value
                                if (!sel.isEmpty()) url.addQueryParameter(filter.key, sel)
                            }
                        }
                    }
                }
                is TextField -> {
                    if (!filter.state.isEmpty()) url.addQueryParameter(filter.key, filter.state)
                }
                is SelectField -> {
                    val sel = filter.values[filter.state].value
                    if (!sel.isEmpty()) url.addQueryParameter(filter.key, sel)
                }
                is Flag -> {
                    val sel = if (filter.state) filter.valTrue else filter.valFalse
                    if (!sel.isEmpty()) url.addQueryParameter(filter.key, sel)
                }
                is OrderBy -> {
                    url.addQueryParameter("order_cond", arrayOf("title", "author", "artist", "rating", "views", "update")[filter.state!!.index])
                    url.addQueryParameter("order", if (filter.state?.ascending == true) "asc" else "desc")
                }
            }
        }
        if (!genres.isEmpty()) url.addQueryParameter("genres", genres)
        url.addQueryParameter("p", page.toString())
        return GET(url.toString(), headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsRequest(manga: SManga): Request {
        val mangaId = manga.url.substringAfterLast("r")
        return GET("$baseUrl/comic_pop?id=$mangaId", headers)
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val tbody = document.select("tbody").first()
        val artistElement = tbody.select("tr:contains(Author/Artist:)").first()

        val manga = SManga.create()
        manga.author = artistElement.selectText("td:eq(1)")
        manga.artist = artistElement.selectText("td:eq(2)") ?: manga.author
        manga.description = tbody.selectText("tr:contains(Description:) > td:eq(1)")
        manga.thumbnail_url = document.select("img[src^=http://img.bato.to/forums/uploads/]").first()?.attr("src")
        manga.status = parseStatus(document.selectText("tr:contains(Status:) > td:eq(1)"))
        manga.genre = tbody.select("tr:contains(Genres:) img").map { it.attr("alt") }.joinToString(", ")
        return manga
    }

    private fun parseStatus(status: String?) = when (status) {
        "Ongoing" -> SManga.ONGOING
        "Complete" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val body = response.body().string()
        val matcher = staffNotice.matcher(body)
        if (matcher.find()) {
            @Suppress("DEPRECATION")
            val notice = Html.fromHtml(matcher.group(1)).toString().trim()
            throw Exception(notice)
        }

        val document = response.asJsoup(body)
        return document.select(chapterListSelector()).map { chapterFromElement(it) }
    }

    override fun chapterListSelector() = "tr.row.lang_English.chapter_row"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a[href^=http://bato.to/reader").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td").getOrNull(4)?.let {
            parseDateFromElement(it)
        } ?: 0
        return chapter
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

    override fun pageListRequest(chapter: SChapter): Request {
        val id = chapter.url.substringAfterLast("#")
        return GET("$baseUrl/areader?id=$id&p=1", pageHeaders)
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
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
        return pages
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

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
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

    private class Status : Filter.TriState("Completed")
    private class Genre(name: String, val id: Int) : Filter.TriState(name)
    private class TextField(name: String, val key: String) : Filter.Text(name)
    private class SelectField(name: String, val key: String, values: Array<ListValue>, state: Int = 0) : Filter.Select<ListValue>(name, values, state)
    private class Flag(name: String, val key: String, val valTrue: String, val valFalse: String) : Filter.CheckBox(name)
    private class GenreList(genres: List<Filter<*>>) : Filter.Group<Filter<*>>("Genres", genres)
    private class OrderBy : Filter.Sort("Order by",
            arrayOf("Title", "Author", "Artist", "Rating", "Views", "Last Update"),
            Filter.Sort.Selection(4, false))

    override fun getFilterList() = FilterList(
            TextField("Author", "artist_name"),
            SelectField("Type", "type", arrayOf(ListValue("Any", ""), ListValue("Manga (Jp)", "jp"), ListValue("Manhwa (Kr)", "kr"), ListValue("Manhua (Cn)", "cn"), ListValue("Artbook", "ar"), ListValue("Other", "ot"))),
            Status(),
            Flag("Exclude mature", "mature", "m", ""),
            OrderBy(),
            GenreList(getGenreList())
    )

    // [...document.querySelectorAll("#advanced_options div.genre_buttons")].map((el,i) => {
    //     const onClick=el.getAttribute('onclick');const id=onClick.substr(14,onClick.length-16);return `Genre("${el.textContent.trim()}", ${id})`
    // }).join(',\n')
    // on https://bato.to/search
    private fun getGenreList() = listOf(
            SelectField("Inclusion mode", "genre_cond", arrayOf(ListValue("And (all selected genres)", "and"), ListValue("Or (any selected genres) ", "or"))),
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