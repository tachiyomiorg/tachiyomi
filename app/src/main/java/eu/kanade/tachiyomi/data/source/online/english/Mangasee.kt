package eu.kanade.tachiyomi.data.source.online.english

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.POST
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class Mangasee(override val id: Int) : ParsedOnlineSource() {

    override val name = "Mangasee"

    override val baseUrl = "http://mangaseeonline.net"

    override val lang = "en"

    override val supportsLatest = true

    private val recentUpdatesPattern = Pattern.compile("(.*?)\\s(\\d+\\.?\\d*)\\s?(Completed)?")

    private val indexPattern = Pattern.compile("-index-(.*?)-")

    override fun popularMangaInitialUrl() = "$baseUrl/search/request.php?sortBy=popularity&sortOrder=descending&todo=1"

    override fun popularMangaSelector() = "div.requested > div.row"

    override fun popularMangaRequest(page: MangasPage): Request {
        if (page.page == 1) {
            page.url = popularMangaInitialUrl()
        }
        val (body, requestUrl) = convertQueryToPost(page)
        return POST(requestUrl, headers, body.build())
    }

    override fun popularMangaParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(popularMangaSelector())) {
            Manga.create(id).apply {
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = page.url
    }

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("a.resultLink").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    // Not used, overrides parent.
    override fun popularMangaNextPageSelector() = ""

    override fun searchMangaInitialUrl(query: String, filters: List<Filter<*>>): String {
        val url = HttpUrl.parse("$baseUrl/search/request.php").newBuilder()
        if (!query.isEmpty()) url.addQueryParameter("keyword", query)
        var genres: String? = null
        var genresNo: String? = null
        for (filter in if (filters.isEmpty()) this@Mangasee.filters else filters) {
            when (filter) {
                is Sort -> filter.values[filter.state].keys.forEachIndexed { i, s ->
                    url.addQueryParameter(s, filter.values[filter.state].values[i])
                }
                is ListField -> if (filter.state != 0) url.addQueryParameter(filter.key, filter.values[filter.state])
                is TextField -> if (!filter.state.isEmpty()) url.addQueryParameter(filter.key, filter.state)
                is Genre -> when (filter.state) {
                    Filter.TriState.STATE_INCLUDE -> genres = if (genres == null) filter.id else genres + "," + filter.id
                    Filter.TriState.STATE_EXCLUDE -> genresNo = if (genresNo == null) filter.id else genresNo + "," + filter.id
                }
            }
        }
        if (genres != null) url.addQueryParameter("genre", genres)
        if (genresNo != null) url.addQueryParameter("genreNo", genresNo)
        return url.toString()
    }

    override fun searchMangaSelector() = "div.searchResults > div.requested > div.row"

    override fun searchMangaRequest(page: MangasPage, query: String, filters: List<Filter<*>>): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query, filters)
        }
        val (body, requestUrl) = convertQueryToPost(page)
        return POST(requestUrl, headers, body.build())
    }

    private fun convertQueryToPost(page: MangasPage): Pair<FormBody.Builder, String> {
        val url = HttpUrl.parse(page.url)
        val body = FormBody.Builder().add("page", page.page.toString())
        for (i in 0..url.querySize() - 1) {
            body.add(url.queryParameterName(i), url.queryParameterValue(i))
        }
        val requestUrl = url.scheme() + "://" + url.host() + url.encodedPath()
        return Pair(body, requestUrl)
    }

    override fun searchMangaParse(response: Response, page: MangasPage, query: String, filters: List<Filter<*>>) {
        val document = response.asJsoup()
        for (element in document.select(popularMangaSelector())) {
            Manga.create(id).apply {
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = page.url
    }

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.select("a.resultLink").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    // Not used, overrides parent.
    override fun searchMangaNextPageSelector() = ""

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val detailElement = document.select("div.well > div.row").first()

        manga.author = detailElement.select("a[href^=/search/?author=]").first()?.text()
        manga.genre = detailElement.select("span.details > div.row > div:has(b:contains(Genre(s))) > a").map { it.text() }.joinToString()
        manga.description = detailElement.select("strong:contains(Description:) + div").first()?.text()
        manga.status = detailElement.select("a[href^=/search/?status=]").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = detailElement.select("div > img").first()?.absUrl("src")
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing (Scan)") -> Manga.ONGOING
        status.contains("Complete (Scan)") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "div.chapter-list > a"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = element.select("span.chapterLabel").first().text()?.let { it } ?: ""
        chapter.date_upload = element.select("time").first()?.attr("datetime")?.let { parseChapterDate(it) } ?: 0
    }

    private fun parseChapterDate(dateAsString: String): Long {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(dateAsString).time
    }

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        val document = response.asJsoup()
        val fullUrl = response.request().url().toString()
        val url = fullUrl.substringBeforeLast('/')

        val series = document.select("input.IndexName").first().attr("value")
        val chapter = document.select("span.CurChapter").first().text()
        var index = ""

        val m = indexPattern.matcher(fullUrl)
        if (m.find()) {
            val indexNumber = m.group(1)
            index = "-index-$indexNumber"
        }

        document.select("div.ContainerNav").first().select("select.PageSelect > option").forEach {
            pages.add(Page(pages.size, "$url/$series-chapter-$chapter$index-page-${pages.size + 1}.html"))
        }
        pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
    }

    // Not used, overrides parent.
    override fun pageListParse(document: Document, pages: MutableList<Page>) {
    }

    override fun imageUrlParse(document: Document): String = document.select("img.CurImage").attr("src")

    private data class SortOption(val name: String, val keys: Array<String>, val values: Array<String>) {
        override fun toString(): String = name
    }

    private class Sort(name: String, values: Array<SortOption>, state: Int = 0) : Filter.List<SortOption>(name, values, state)
    private class Genre(name: String, val id: String = name.replace(' ', '_')) : Filter.TriState(name)
    private class TextField(name: String, val key: String) : Filter.Text(name)
    private class ListField(name: String, val key: String, values: Array<String>, state: Int = 0) : Filter.List<String>(name, values, state)

    // [...document.querySelectorAll("label.triStateCheckBox input")].map(el => `Filter("${el.getAttribute('name')}", "${el.nextSibling.textContent.trim()}")`).join(',\n')
    // http://mangasee.co/advanced-search/
    override fun getFilterList(): List<Filter<*>> = listOf(
            TextField("Years", "year"),
            TextField("Author", "author"),
            Sort("Sort By", arrayOf(SortOption("Alphabetical A-Z", emptyArray(), emptyArray()),
                    SortOption("Alphabetical Z-A", arrayOf("sortOrder"), arrayOf("descending")),
                    SortOption("Newest", arrayOf("sortBy", "sortOrder"), arrayOf("dateUpdated", "descending")),
                    SortOption("Oldest", arrayOf("sortBy"), arrayOf("dateUpdated")),
                    SortOption("Most Popular", arrayOf("sortBy", "sortOrder"), arrayOf("popularity", "descending")),
                    SortOption("Least Popular", arrayOf("sortBy"), arrayOf("popularity"))
            ), 4),
            ListField("Scan Status", "status", arrayOf("Any", "Complete", "Discontinued", "Hiatus", "Incomplete", "Ongoing")),
            ListField("Publish Status", "pstatus", arrayOf("Any", "Cancelled", "Complete", "Discontinued", "Hiatus", "Incomplete", "Ongoing", "Unfinished")),
            ListField("Type", "type", arrayOf("Any", "Doujinshi", "Manga", "Manhua", "Manhwa", "OEL", "One-shot")),
            Filter.Header("Genres"),
            Genre("Action"),
            Genre("Adult"),
            Genre("Adventure"),
            Genre("Comedy"),
            Genre("Doujinshi"),
            Genre("Drama"),
            Genre("Ecchi"),
            Genre("Fantasy"),
            Genre("Gender Bender"),
            Genre("Harem"),
            Genre("Hentai"),
            Genre("Historical"),
            Genre("Horror"),
            Genre("Josei"),
            Genre("Lolicon"),
            Genre("Martial Arts"),
            Genre("Mature"),
            Genre("Mecha"),
            Genre("Mystery"),
            Genre("Psychological"),
            Genre("Romance"),
            Genre("School Life"),
            Genre("Sci-fi"),
            Genre("Seinen"),
            Genre("Shotacon"),
            Genre("Shoujo"),
            Genre("Shoujo Ai"),
            Genre("Shounen"),
            Genre("Shounen Ai"),
            Genre("Slice of Life"),
            Genre("Smut"),
            Genre("Sports"),
            Genre("Supernatural"),
            Genre("Tragedy"),
            Genre("Yaoi"),
            Genre("Yuri")
    )

    override fun latestUpdatesInitialUrl(): String = "http://mangaseeonline.net/home/latest.request.php"

    // Not used, overrides parent.
    override fun latestUpdatesNextPageSelector(): String = ""

    override fun latestUpdatesSelector(): String = "a.latestSeries"

    override fun latestUpdatesRequest(page: MangasPage): Request {
        if (page.page == 1) {
            page.url = latestUpdatesInitialUrl()
        }
        val (body, requestUrl) = convertQueryToPost(page)
        return POST(requestUrl, headers, body.build())
    }

    override fun latestUpdatesParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(latestUpdatesSelector())) {
            Manga.create(id).apply {
                latestUpdatesFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = page.url
    }

    override fun latestUpdatesFromElement(element: Element, manga: Manga) {
        element.select("a.latestSeries").first().let {
            val chapterUrl = it.attr("href")
            val indexOfMangaUrl = chapterUrl.indexOf("-chapter-")
            val indexOfLastPath = chapterUrl.lastIndexOf("/")
            val mangaUrl = chapterUrl.substring(indexOfLastPath, indexOfMangaUrl)
            val defaultText = it.select("p.clamp2").text()
            val m = recentUpdatesPattern.matcher(defaultText)
            val title = if (m.matches()) m.group(1) else defaultText
            manga.setUrlWithoutDomain("/manga" + mangaUrl)
            manga.title = title
        }
    }

}