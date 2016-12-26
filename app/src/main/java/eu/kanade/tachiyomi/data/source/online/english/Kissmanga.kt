package eu.kanade.tachiyomi.data.source.online.english

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.POST
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class Kissmanga(override val id: Int) : ParsedOnlineSource() {

    override val name = "Kissmanga"

    override val baseUrl = "http://kissmanga.com"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun popularMangaInitialUrl() = "$baseUrl/MangaList/MostPopular"

    override fun latestUpdatesInitialUrl() = "http://kissmanga.com/MangaList/LatestUpdate"

    override fun popularMangaSelector() = "table.listing tr:gt(1)"

    override fun latestUpdatesSelector() = "table.listing tr:gt(1)"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("td a:eq(0)").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun latestUpdatesFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun popularMangaNextPageSelector() = "li > a:contains(› Next)"

    override fun latestUpdatesNextPageSelector(): String = "ul.pager > li > a:contains(Next)"

    override fun searchMangaRequest(page: MangasPage, query: String, filters: List<Filter>): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query, filters)
        }

        val form = FormBody.Builder().apply {
            add("authorArtist", "")
            add("mangaName", query)

            this@Kissmanga.filters.forEach { filter ->
                if (filter.equals(completedFilter)) add("status", if (filter in filters) filter.id else "")
                else add("genres", if (filter in filters) "1" else "0")
            }
        }

        return POST(page.url, headers, form.build())
    }

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) = "$baseUrl/AdvanceSearch"

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = null

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val infoElement = document.select("div.barContent").first()

        manga.author = infoElement.select("p:has(span:contains(Author:)) > a").first()?.text()
        manga.genre = infoElement.select("p:has(span:contains(Genres:)) > *:gt(0)").text()
        manga.description = infoElement.select("p:has(span:contains(Summary:)) ~ p").text()
        manga.status = infoElement.select("p:has(span:contains(Status:))").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = document.select(".rightBox:eq(0) img").first()?.attr("src")
    }

    fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "table.listing tr:gt(1)"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td:eq(1)").first()?.text()?.let {
            SimpleDateFormat("MM/dd/yyyy").parse(it).time
        } ?: 0
    }

    override fun pageListRequest(chapter: Chapter) = POST(baseUrl + chapter.url, headers)

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        //language=RegExp
        val p = Pattern.compile("""lstImages.push\("(.+?)"""")
        val m = p.matcher(response.body().string())

        var i = 0
        while (m.find()) {
            pages.add(Page(i++, "", m.group(1)))
        }
    }

    // Not used
    override fun pageListParse(document: Document, pages: MutableList<Page>) {
    }

    override fun imageUrlRequest(page: Page) = GET(page.url)

    override fun imageUrlParse(document: Document) = ""

    private val completedFilter = Filter("Completed", "Completed")
    // $("select[name=\"genres\"]").map((i,el) => `Filter("${i}", "${$(el).next().text().trim()}")`).get().join(',\n')
    // on http://kissmanga.com/AdvanceSearch
    override fun getFilterList(): List<Filter> = listOf(
            completedFilter,
            Filter("0", "Action"),
            Filter("1", "Adult"),
            Filter("2", "Adventure"),
            Filter("3", "Comedy"),
            Filter("4", "Comic"),
            Filter("5", "Cooking"),
            Filter("6", "Doujinshi"),
            Filter("7", "Drama"),
            Filter("8", "Ecchi"),
            Filter("9", "Fantasy"),
            Filter("10", "Gender Bender"),
            Filter("11", "Harem"),
            Filter("12", "Historical"),
            Filter("13", "Horror"),
            Filter("14", "Josei"),
            Filter("15", "Lolicon"),
            Filter("16", "Manga"),
            Filter("17", "Manhua"),
            Filter("18", "Manhwa"),
            Filter("19", "Martial Arts"),
            Filter("20", "Mature"),
            Filter("21", "Mecha"),
            Filter("22", "Medical"),
            Filter("23", "Music"),
            Filter("24", "Mystery"),
            Filter("25", "One shot"),
            Filter("26", "Psychological"),
            Filter("27", "Romance"),
            Filter("28", "School Life"),
            Filter("29", "Sci-fi"),
            Filter("30", "Seinen"),
            Filter("31", "Shotacon"),
            Filter("32", "Shoujo"),
            Filter("33", "Shoujo Ai"),
            Filter("34", "Shounen"),
            Filter("35", "Shounen Ai"),
            Filter("36", "Slice of Life"),
            Filter("37", "Smut"),
            Filter("38", "Sports"),
            Filter("39", "Supernatural"),
            Filter("40", "Tragedy"),
            Filter("41", "Webtoon"),
            Filter("42", "Yaoi"),
            Filter("43", "Yuri")
    )
}