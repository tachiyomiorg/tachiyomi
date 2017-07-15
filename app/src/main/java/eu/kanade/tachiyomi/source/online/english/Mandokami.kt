package eu.kanade.tachiyomi.source.online.english

import com.squareup.duktape.Duktape
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class Mandokami : ParsedHttpSource(), LoginSource {

    override val id: Long = 4

    override val name = "Mandokami"

    override val baseUrl = "https://manga.madokami.al/Manga"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun popularMangaSelector() = "table.listing tr:gt(1)"

    override fun latestUpdatesSelector() = "table.listing tr:gt(1)"

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/MangaList/MostPopular?page=$page", headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("https://manga.madokami.al/recent", headers)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("td a:eq(0)").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun popularMangaNextPageSelector() = "li > a:contains(› Next)"

    override fun latestUpdatesNextPageSelector(): String = "ul.pager > li > a:contains(Next)"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val form = FormBody.Builder().apply {
            add("mangaName", query)

            for (filter in if (filters.isEmpty()) getFilterList() else filters) {
                when (filter) {
                    is Author -> add("authorArtist", filter.state)
                    is Status -> add("status", arrayOf("", "Completed", "Ongoing")[filter.state])
                    is GenreList -> filter.state.forEach { genre -> add("genres", genre.state.toString()) }
                }
            }
        }
        return POST("$baseUrl/AdvanceSearch", headers, form.build())
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun searchMangaNextPageSelector() = null

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("div.barContent").first()

        val manga = SManga.create()
        manga.author = infoElement.select("p:has(span:contains(Author:)) > a").first()?.text()
        manga.genre = infoElement.select("p:has(span:contains(Genres:)) > *:gt(0)").text()
        manga.description = infoElement.select("p:has(span:contains(Summary:)) ~ p").text()
        manga.status = infoElement.select("p:has(span:contains(Status:))").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = document.select(".rightBox:eq(0) img").first()?.attr("src")
        return manga
    }

    fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> SManga.ONGOING
        status.contains("Completed") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = "table.listing tr:gt(1)"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td:eq(1)").first()?.text()?.let {
            SimpleDateFormat("MM/dd/yyyy").parse(it).time
        } ?: 0
        return chapter
    }

    override fun pageListRequest(chapter: SChapter) = POST(baseUrl + chapter.url, headers)

    override fun pageListParse(response: Response): List<Page> {
        val body = response.body()!!.string()

        val pages = mutableListOf<Page>()

        // Kissmanga now encrypts the urls, so we need to execute these two scripts in JS.
        val ca = client.newCall(GET("$baseUrl/Scripts/ca.js", headers)).execute().body()!!.string()
        val lo = client.newCall(GET("$baseUrl/Scripts/lo.js", headers)).execute().body()!!.string()

        Duktape.create().use {
            it.evaluate(ca)
            it.evaluate(lo)

            // There are two functions in an inline script needed to decrypt the urls. We find and
            // execute them.
            var p = Pattern.compile("(.*CryptoJS.*)")
            var m = p.matcher(body)
            while (m.find()) {
                it.evaluate(m.group(1))
            }

            // Finally find all the urls and decrypt them in JS.
            p = Pattern.compile("""lstImages.push\((.*)\);""")
            m = p.matcher(body)

            var i = 0
            while (m.find()) {
                val url = it.evaluate(m.group(1)) as String
                pages.add(Page(i++, "", url))
            }
        }

        return pages
    }

    override fun pageListParse(document: Document): List<Page> {
        throw Exception("Not used")
    }

    override fun imageUrlRequest(page: Page) = GET(page.url)

    override fun imageUrlParse(document: Document) = ""

    private class Status : Filter.TriState("Completed")
    private class Author : Filter.Text("Author")
    private class Genre(name: String) : Filter.TriState(name)
    private class GenreList(genres: List<Genre>) : Filter.Group<Genre>("Genres", genres)

    override fun getFilterList() = FilterList(
            Author(),
            Status(),
            GenreList(getGenreList())
    )

    // $("select[name=\"genres\"]").map((i,el) => `Genre("${$(el).next().text().trim()}", ${i})`).get().join(',\n')
    // on http://kissmanga.com/AdvanceSearch
    private fun getGenreList() = listOf(
            Genre("4-Koma"),
            Genre("Action"),
            Genre("Adult"),
            Genre("Adventure"),
            Genre("Comedy"),
            Genre("Comic"),
            Genre("Cooking"),
            Genre("Doujinshi"),
            Genre("Drama"),
            Genre("Ecchi"),
            Genre("Fantasy"),
            Genre("Gender Bender"),
            Genre("Harem"),
            Genre("Historical"),
            Genre("Horror"),
            Genre("Josei"),
            Genre("Lolicon"),
            Genre("Manga"),
            Genre("Manhua"),
            Genre("Manhwa"),
            Genre("Martial Arts"),
            Genre("Mature"),
            Genre("Mecha"),
            Genre("Medical"),
            Genre("Music"),
            Genre("Mystery"),
            Genre("One shot"),
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
            Genre("Webtoon"),
            Genre("Yaoi"),
            Genre("Yuri")
    )
}
