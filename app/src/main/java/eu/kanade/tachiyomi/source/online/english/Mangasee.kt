package eu.kanade.tachiyomi.source.online.english

import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class Mangasee : ParsedHttpSource() {

    override val id: Long = 9

    override val name = "Mangasee"

    override val baseUrl = "http://mangaseeonline.net"

    override val lang = "en"

    override val supportsLatest = true

    private val recentUpdatesPattern = Pattern.compile("(.*?)\\s(\\d+\\.?\\d*)\\s?(Completed)?")

    private val indexPattern = Pattern.compile("-index-(.*?)-")

    private val catalogHeaders = Headers.Builder().apply {
        add("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
        add("Host", "mangaseeonline.us")
    }.build()

    override fun popularMangaSelector() = "div.requested > div.row"

    override fun popularMangaRequest(page: Int): Request {
        val (body, requestUrl) = convertQueryToPost(page, "$baseUrl/search/request.php?sortBy=popularity&sortOrder=descending")
        return POST(requestUrl, catalogHeaders, body.build())
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a.resultLink").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun popularMangaNextPageSelector() = "button.requestMore"

    override fun searchMangaSelector() = "div.requested > div.row"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = HttpUrl.parse("$baseUrl/search/request.php").newBuilder()
        if (!query.isEmpty()) url.addQueryParameter("keyword", query)
        val genres = mutableListOf<String>()
        val genresNo = mutableListOf<String>()
        for (filter in if (filters.isEmpty()) getFilterList() else filters) {
            when (filter) {
                is Sort -> {
                    if (filter.state?.index != 0)
                        url.addQueryParameter("sortBy", if (filter.state?.index == 1) "dateUpdated" else "popularity")
                    if (filter.state?.ascending != true)
                        url.addQueryParameter("sortOrder", "descending")
                }
                is SelectField -> if (filter.state != 0) url.addQueryParameter(filter.key, filter.values[filter.state])
                is TextField -> if (!filter.state.isEmpty()) url.addQueryParameter(filter.key, filter.state)
                is GenreList -> filter.state.forEach { genre ->
                    when (genre.state) {
                        Filter.TriState.STATE_INCLUDE -> genres.add(genre.name)
                        Filter.TriState.STATE_EXCLUDE -> genresNo.add(genre.name)
                    }
                }
            }
        }
        if (genres.isNotEmpty()) url.addQueryParameter("genre", genres.joinToString(","))
        if (genresNo.isNotEmpty()) url.addQueryParameter("genreNo", genresNo.joinToString(","))

        val (body, requestUrl) = convertQueryToPost(page, url.toString())
        return POST(requestUrl, catalogHeaders, body.build())
    }

    private fun convertQueryToPost(page: Int, url: String): Pair<FormBody.Builder, String> {
        val url = HttpUrl.parse(url)
        val body = FormBody.Builder().add("page", page.toString())
        for (i in 0..url.querySize() - 1) {
            body.add(url.queryParameterName(i), url.queryParameterValue(i))
        }
        val requestUrl = url.scheme() + "://" + url.host() + url.encodedPath()
        return Pair(body, requestUrl)
    }

    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a.resultLink").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun searchMangaNextPageSelector() = "button.requestMore"

    override fun mangaDetailsParse(document: Document): SManga {
        val detailElement = document.select("div.well > div.row").first()

        val manga = SManga.create()
        manga.author = detailElement.select("a[href^=/search/?author=]").first()?.text()
        manga.genre = detailElement.select("span.details > div.row > div:has(b:contains(Genre(s))) > a").map { it.text() }.joinToString()
        manga.description = detailElement.select("strong:contains(Description:) + div").first()?.text()
        manga.status = detailElement.select("a[href^=/search/?status=]").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = detailElement.select("div > img").first()?.absUrl("src")
        return manga
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing (Scan)") -> SManga.ONGOING
        status.contains("Complete (Scan)") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = "div.chapter-list > a"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = element.select("span.chapterLabel").first().text()?.let { it } ?: ""
        chapter.date_upload = element.select("time").first()?.attr("datetime")?.let { parseChapterDate(it) } ?: 0
        return chapter
    }

    private fun parseChapterDate(dateAsString: String): Long {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(dateAsString).time
    }

    override fun pageListParse(document: Document): List<Page> {
        val fullUrl = document.baseUri()
        val url = fullUrl.substringBeforeLast('/')

        val pages = mutableListOf<Page>()

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
        return pages
    }

    override fun imageUrlParse(document: Document): String = document.select("img.CurImage").attr("src")

    override fun latestUpdatesNextPageSelector() = "button.requestMore"

    override fun latestUpdatesSelector(): String = "a.latestSeries"

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "http://mangaseeonline.net/home/latest.request.php"
        val (body, requestUrl) = convertQueryToPost(page, url)
        return POST(requestUrl, catalogHeaders, body.build())
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
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
        return manga
    }

    private class Sort : Filter.Sort("Sort", arrayOf("Alphabetically", "Date updated", "Popularity"), Filter.Sort.Selection(2, false))
    private class Genre(name: String) : Filter.TriState(name)
    private class TextField(name: String, val key: String) : Filter.Text(name)
    private class SelectField(name: String, val key: String, values: Array<String>, state: Int = 0) : Filter.Select<String>(name, values, state)
    private class GenreList(genres: List<Genre>) : Filter.Group<Genre>("Genres", genres)

    override fun getFilterList() = FilterList(
            TextField("Years", "year"),
            TextField("Author", "author"),
            SelectField("Scan Status", "status", arrayOf("Any", "Complete", "Discontinued", "Hiatus", "Incomplete", "Ongoing")),
            SelectField("Publish Status", "pstatus", arrayOf("Any", "Cancelled", "Complete", "Discontinued", "Hiatus", "Incomplete", "Ongoing", "Unfinished")),
            SelectField("Type", "type", arrayOf("Any", "Doujinshi", "Manga", "Manhua", "Manhwa", "OEL", "One-shot")),
            Sort(),
            GenreList(getGenreList())
    )

    // [...document.querySelectorAll("label.triStateCheckBox input")].map(el => `Filter("${el.getAttribute('name')}", "${el.nextSibling.textContent.trim()}")`).join(',\n')
    // http://mangasee.co/advanced-search/
    private fun getGenreList() = listOf(
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

}