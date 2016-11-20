package eu.kanade.tachiyomi.data.source.online.english

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.POST
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
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

    override val lang: Language get() = EN

    override val supportsLatest = true

    private val recentUpdatesPattern = Pattern.compile("(.*?)\\s(\\d+\\.?\\d*)\\s?(Completed)?")

    private val indexPattern = Pattern.compile("-index-(.*?)-")

    override fun popularMangaInitialUrl() = "$baseUrl/search/request.php?sortBy=popularity&sortOrder=descending"

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

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/search/request.php?sortBy=popularity&sortOrder=descending&keyword=$query&genre=${filters.map { it.id }.joinToString(",")}"

    override fun searchMangaSelector() = "div.searchResults > div.requested > div.row"

    override fun searchMangaRequest(page: MangasPage, query: String, filters: List<Filter>): Request {
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

    override fun searchMangaParse(response: Response, page: MangasPage, query: String, filters: List<Filter>) {
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

    // [...document.querySelectorAll("label.triStateCheckBox input")].map(el => `Filter("${el.getAttribute('name')}", "${el.nextSibling.textContent.trim()}")`).join(',\n')
    // http://mangasee.co/advanced-search/
    override fun getFilterList(): List<Filter> = listOf(
            Filter("Action", "Action"),
            Filter("Adult", "Adult"),
            Filter("Adventure", "Adventure"),
            Filter("Comedy", "Comedy"),
            Filter("Doujinshi", "Doujinshi"),
            Filter("Drama", "Drama"),
            Filter("Ecchi", "Ecchi"),
            Filter("Fantasy", "Fantasy"),
            Filter("Gender_Bender", "Gender Bender"),
            Filter("Harem", "Harem"),
            Filter("Hentai", "Hentai"),
            Filter("Historical", "Historical"),
            Filter("Horror", "Horror"),
            Filter("Josei", "Josei"),
            Filter("Lolicon", "Lolicon"),
            Filter("Martial_Arts", "Martial Arts"),
            Filter("Mature", "Mature"),
            Filter("Mecha", "Mecha"),
            Filter("Mystery", "Mystery"),
            Filter("Psychological", "Psychological"),
            Filter("Romance", "Romance"),
            Filter("School_Life", "School Life"),
            Filter("Sci-fi", "Sci-fi"),
            Filter("Seinen", "Seinen"),
            Filter("Shotacon", "Shotacon"),
            Filter("Shoujo", "Shoujo"),
            Filter("Shoujo_Ai", "Shoujo Ai"),
            Filter("Shounen", "Shounen"),
            Filter("Shounen_Ai", "Shounen Ai"),
            Filter("Slice_of_Life", "Slice of Life"),
            Filter("Smut", "Smut"),
            Filter("Sports", "Sports"),
            Filter("Supernatural", "Supernatural"),
            Filter("Tragedy", "Tragedy"),
            Filter("Yaoi", "Yaoi"),
            Filter("Yuri", "Yuri")
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
