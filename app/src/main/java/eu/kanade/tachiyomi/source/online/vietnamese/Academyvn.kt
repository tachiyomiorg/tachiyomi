package eu.kanade.tachiyomi.source.online.vietnamese

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*

class Academyvn : ParsedHttpSource() {

    override val id: Long = 11

    override val name = "Academy VN"

    override val baseUrl = "http://truyen.academyvn.com"

    override val lang = "vi"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun popularMangaSelector() = "table.table.table-hover > tbody > tr"

    override fun latestUpdatesSelector() = "table.table.table-hover > tbody > tr"

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga/all?filter_type=view&page=$page", headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("http://truyen.academyvn.com/manga/all?filter_type=latest-chapter&page=$page", headers)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.attr("title")
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun popularMangaNextPageSelector() = "li > a:contains(»)"

    override fun latestUpdatesNextPageSelector(): String = "li > a:contains(»)"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = HttpUrl.parse("$baseUrl/searchs?").newBuilder().addQueryParameter("keyword", query)
        var genreIndex = 1
        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is Type -> url.addQueryParameter("type", if (filter.state == 0) "-1" else type.indexOf(filter.state.toString()).toString())
                is Author -> url.addQueryParameter("author", "-1")
                is GenreList -> filter.state.forEach {
                    genre ->
                    if (genre.state != 0) url.addQueryParameter("genres[]", genreIndex.toString())
                    genreIndex++
                }
                is Status -> url.addQueryParameter("status", if (filter.state == 0) "-1" else status.indexOf(filter.state.toString()).toString())
            }
        }
        url.addQueryParameter("submit", "Tìm+kiếm")
        url.addQueryParameter("page", page.toString())
        return GET(url.toString(), headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun searchMangaNextPageSelector() = "li > a:contains(»)"

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("div.__info-container").first()

        val manga = SManga.create()
        manga.author = infoElement.select("p:has(strong:contains(Tác giả:)) > a").first()?.text()
        manga.genre = infoElement.select("p:has(strong:contains(Thể loại:)) > *:gt(0)").text()
        manga.description = infoElement.select("div.__description > p").text()
        manga.status = infoElement.select("p:has(strong:contains(Tình trạng:))").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = document.select("div.__left img").first()?.attr("src")
        return manga
    }

    fun parseStatus(status: String) = when {
        status.contains("Đang tiến hành") -> SManga.ONGOING
        status.contains("Đã hoàn thành") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = "div.table-scroll > table.table.table-hover > tbody > tr"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()
        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.attr("title")
        chapter.date_upload = element.select("td.text-center").last()?.text()?.let { parseChapterDate(it) } ?: 0
        return chapter
    }

    private fun parseChapterDate(date: String): Long {
        val dateWords: List<String> = date.split(" ")
        if (dateWords.size == 3) {
            val timeAgo = Integer.parseInt(dateWords[0])
            val dates: Calendar = Calendar.getInstance()
            if (dateWords[1].contains("minutes") || dateWords[1].contains("minute")) {
                dates.add(Calendar.MINUTE, -timeAgo)
            } else if (dateWords[1].contains("hours") || dateWords[1].contains("hour")) {
                dates.add(Calendar.HOUR_OF_DAY, -timeAgo)
            } else if (dateWords[1].contains("days") || dateWords[1].contains("day")) {
                dates.add(Calendar.DAY_OF_YEAR, -timeAgo)
            } else if (dateWords[1].contains("weeks") || dateWords[1].contains("week")) {
                dates.add(Calendar.WEEK_OF_YEAR, -timeAgo)
            } else if (dateWords[1].contains("months") || dateWords[1].contains("month")) {
                dates.add(Calendar.MONTH, -timeAgo)
            } else if (dateWords[1].contains("years") || dateWords[1].contains("year")) {
                dates.add(Calendar.YEAR, -timeAgo)
            }
            return dates.timeInMillis
        }
        return 0L
    }

    override fun pageListRequest(chapter: SChapter) = GET(baseUrl + chapter.url, headers)

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        var i = 0
        document.select("div.manga-container > img").forEach {
            pages.add(Page(i++, "", it.attr("src")))
        }
        return pages
    }

    override fun imageUrlRequest(page: Page) = GET(page.url)

    override fun imageUrlParse(document: Document) = ""

    var type = arrayOf("Khác", "Manga", "Manhwa", "Manhua", "Tất cả")
    var status = arrayOf("Ngưng", "Đang tiến hành", "Đã hoàn thành")

    private class Type : Filter.Select<String>("Type", arrayOf("Khác", "Manga", "Manhwa", "Manhua", "Tất cả"))
    private class Status : Filter.Select<String>("Status", arrayOf("Ngưng", "Đang tiến hành", "Đã hoàn thành"))
    private class Author : Filter.Text("Tác giả")
    private class Genre(name: String) : Filter.TriState(name)
    private class GenreList(genres: List<Genre>) : Filter.Group<Genre>("Thể loại", genres)

    override fun getFilterList() = FilterList(
            Type(),
            Status(),
            GenreList(getGenreList())
    )

    // $("select[name=\"genres\"]").map((i,el) => `Genre("${$(el).next().text().trim()}", ${i})`).get().join(',\n')
    // on http://kissmanga.com/AdvanceSearch
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
            Genre("Historical"),
            Genre("Horror"),
            Genre("Josei"),
            Genre("Martial Arts"),
            Genre("Mature"),
            Genre("Mecha"),
            Genre("Mystery"),
            Genre("One shot"),
            Genre("Psychological"),
            Genre("Romance"),
            Genre("School Life"),
            Genre("Sci-fi"),
            Genre("Seinen"),
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
            Genre("Yuri"),
            Genre("Hot")
    )
}