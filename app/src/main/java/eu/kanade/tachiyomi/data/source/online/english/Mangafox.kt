package eu.kanade.tachiyomi.data.source.online.english

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class Mangafox(override val id: Int) : ParsedOnlineSource() {

    override val name = "Mangafox"

    override val baseUrl = "http://mangafox.me"

    override val lang = "en"

    override val supportsLatest = true

    override fun popularMangaInitialUrl() = "$baseUrl/directory/"

    override fun latestUpdatesInitialUrl() = "$baseUrl/directory/?latest"

    override fun popularMangaSelector() = "div#mangalist > ul.list > li"

    override fun latestUpdatesSelector() = "div#mangalist > ul.list > li"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("a.title").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun latestUpdatesFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun popularMangaNextPageSelector() = "a:has(span.next)"

    override fun latestUpdatesNextPageSelector() = "a:has(span.next)"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/search.php?name_method=cw&advopts=1&order=za&sort=views&name=$query&page=1&${filters.map { it.id + "=1" }.joinToString("&")}"

    override fun searchMangaSelector() = "div#mangalist > ul.list > li"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.select("a.title").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun searchMangaNextPageSelector() = "a:has(span.next)"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val infoElement = document.select("div#title").first()
        val rowElement = infoElement.select("table > tbody > tr:eq(1)").first()
        val sideInfoElement = document.select("#series_info").first()

        manga.author = rowElement.select("td:eq(1)").first()?.text()
        manga.artist = rowElement.select("td:eq(2)").first()?.text()
        manga.genre = rowElement.select("td:eq(3)").first()?.text()
        manga.description = infoElement.select("p.summary").first()?.text()
        manga.status = sideInfoElement.select(".data").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = sideInfoElement.select("div.cover > img").first()?.attr("src")
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "div#chapters li div"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a.tips").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("span.date").first()?.text()?.let { parseChapterDate(it) } ?: 0
    }

    private fun parseChapterDate(date: String): Long {
        return if ("Today" in date || " ago" in date) {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else if ("Yesterday" in date) {
            Calendar.getInstance().apply {
                add(Calendar.DATE, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            try {
                SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).parse(date).time
            } catch (e: ParseException) {
                0L
            }
        }
    }

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        val document = response.asJsoup()

        val url = response.request().url().toString().substringBeforeLast('/')
        document.select("select.m").first()?.select("option:not([value=0])")?.forEach {
            pages.add(Page(pages.size, "$url/${it.attr("value")}.html"))
        }
    }

    // Not used, overrides parent.
    override fun pageListParse(document: Document, pages: MutableList<Page>) {}

    override fun imageUrlParse(document: Document) = document.getElementById("image").attr("src")

    // $('select.genres').map((i,el)=>`Filter("${$(el).attr('name')}", "${$(el).next().text().trim()}")`).get().join(',\n')
    // on http://kissmanga.com/AdvanceSearch
    override fun getFilterList(): List<Filter> = listOf(
            Filter("is_completed", "Completed"),
            Filter("genres[Action]", "Action"),
            Filter("genres[Adult]", "Adult"),
            Filter("genres[Adventure]", "Adventure"),
            Filter("genres[Comedy]", "Comedy"),
            Filter("genres[Doujinshi]", "Doujinshi"),
            Filter("genres[Drama]", "Drama"),
            Filter("genres[Ecchi]", "Ecchi"),
            Filter("genres[Fantasy]", "Fantasy"),
            Filter("genres[Gender Bender]", "Gender Bender"),
            Filter("genres[Harem]", "Harem"),
            Filter("genres[Historical]", "Historical"),
            Filter("genres[Horror]", "Horror"),
            Filter("genres[Josei]", "Josei"),
            Filter("genres[Martial Arts]", "Martial Arts"),
            Filter("genres[Mature]", "Mature"),
            Filter("genres[Mecha]", "Mecha"),
            Filter("genres[Mystery]", "Mystery"),
            Filter("genres[One Shot]", "One Shot"),
            Filter("genres[Psychological]", "Psychological"),
            Filter("genres[Romance]", "Romance"),
            Filter("genres[School Life]", "School Life"),
            Filter("genres[Sci-fi]", "Sci-fi"),
            Filter("genres[Seinen]", "Seinen"),
            Filter("genres[Shoujo]", "Shoujo"),
            Filter("genres[Shoujo Ai]", "Shoujo Ai"),
            Filter("genres[Shounen]", "Shounen"),
            Filter("genres[Shounen Ai]", "Shounen Ai"),
            Filter("genres[Slice of Life]", "Slice of Life"),
            Filter("genres[Smut]", "Smut"),
            Filter("genres[Sports]", "Sports"),
            Filter("genres[Supernatural]", "Supernatural"),
            Filter("genres[Tragedy]", "Tragedy"),
            Filter("genres[Webtoons]", "Webtoons"),
            Filter("genres[Yaoi]", "Yaoi"),
            Filter("genres[Yuri]", "Yuri")
    )

}