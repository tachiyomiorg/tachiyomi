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

    override fun searchMangaRequest(page: MangasPage, query: String, filters: List<Filter<*>>): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query, filters)
        }

        val form = FormBody.Builder().apply {
            add("mangaName", query)

            for (filter in if (filters.isEmpty()) this@Kissmanga.filters else filters) {
                when (filter) {
                    is Author -> add("authorArtist", filter.state)
                    is Status -> add("status", arrayOf("", "Completed", "Ongoing")[filter.state])
                    is Genre -> add("genres", filter.state.toString())
                }
            }
        }
        return POST(page.url, headers, form.build())
    }

    override fun searchMangaInitialUrl(query: String, filters: List<Filter<*>>) = "$baseUrl/AdvanceSearch"

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

    private class Status() : Filter.TriState("Completed")
    private class Author() : Filter.Text("Author")
    private class Genre(name: String, val id: Int) : Filter.TriState(name)

    // $("select[name=\"genres\"]").map((i,el) => `Genre("${$(el).next().text().trim()}", ${i})`).get().join(',\n')
    // on http://kissmanga.com/AdvanceSearch
    override fun getFilterList(): List<Filter<*>> = listOf(
            Author(),
            Status(),
            Filter.Header("Genres"),
            Genre("Action", 0),
            Genre("Adult", 1),
            Genre("Adventure", 2),
            Genre("Comedy", 3),
            Genre("Comic", 4),
            Genre("Cooking", 5),
            Genre("Doujinshi", 6),
            Genre("Drama", 7),
            Genre("Ecchi", 8),
            Genre("Fantasy", 9),
            Genre("Gender Bender", 10),
            Genre("Harem", 11),
            Genre("Historical", 12),
            Genre("Horror", 13),
            Genre("Josei", 14),
            Genre("Lolicon", 15),
            Genre("Manga", 16),
            Genre("Manhua", 17),
            Genre("Manhwa", 18),
            Genre("Martial Arts", 19),
            Genre("Mature", 20),
            Genre("Mecha", 21),
            Genre("Medical", 22),
            Genre("Music", 23),
            Genre("Mystery", 24),
            Genre("One shot", 25),
            Genre("Psychological", 26),
            Genre("Romance", 27),
            Genre("School Life", 28),
            Genre("Sci-fi", 29),
            Genre("Seinen", 30),
            Genre("Shotacon", 31),
            Genre("Shoujo", 32),
            Genre("Shoujo Ai", 33),
            Genre("Shounen", 34),
            Genre("Shounen Ai", 35),
            Genre("Slice of Life", 36),
            Genre("Smut", 37),
            Genre("Sports", 38),
            Genre("Supernatural", 39),
            Genre("Tragedy", 40),
            Genre("Webtoon", 41),
            Genre("Yaoi", 42),
            Genre("Yuri", 43)
    )
}