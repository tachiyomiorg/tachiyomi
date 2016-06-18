package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*
import java.util.regex.Pattern

class Mangasee(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Mangasee"

    override val baseUrl = "http://www.mangasee.co"

    override val lang: Language get() = EN

    private val datePattern = Pattern.compile("(\\d+)\\s+(.*?)s? ago.*")

    private val dateFields = HashMap<String, Int>().apply {
        put("second", Calendar.SECOND)
        put("minute", Calendar.MINUTE)
        put("hour", Calendar.HOUR)
        put("day", Calendar.DATE)
        put("week", Calendar.WEEK_OF_YEAR)
        put("month", Calendar.MONTH)
        put("year", Calendar.YEAR)
    }

    override fun popularMangaInitialUrl() = "$baseUrl/search_result.php?Action=Yes&order=popularity&numResultPerPage=20&sort=desc"

    override fun popularMangaSelector() = "div.well > table > tbody > tr"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("td > h2 > a").first().let {
            manga.setUrlWithoutDomain("/${it.attr("href")}")
            manga.title = it.text()
        }
    }

    override fun popularMangaNextPageSelector() = "ul.pagination > li > a:contains(Next)"

    override fun searchMangaInitialUrl(query: String) =
            "$baseUrl/advanced-search/result.php?sortBy=alphabet&direction=ASC&textOnly=no&resPerPage=20&page=1&seriesName=$query"

    override fun searchMangaSelector() = "div.row > div > div > div > h1"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.select("a").first().let {
            manga.setUrlWithoutDomain("/${it.attr("href")}")
            manga.title = it.text()
        }
    }

    override fun searchMangaNextPageSelector() = "ul.pagination > li > a:contains(Next)"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val detailElement = document.select("div.well > div.row").first()

        manga.author = detailElement.select("a[href^=../search_result.php?author_name=]").first()?.text()
        manga.genre = detailElement.select("div > div.row > div:has(b:contains(Genre:)) > a").map { it.text() }.joinToString()
        manga.description = detailElement.select("strong:contains(Description:) + div").first()?.text()
        manga.status = detailElement.select("div > div.row > div:has(b:contains(Scanlation Status:))").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = detailElement.select("div > img").first()?.absUrl("src")
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "div.row > div > div.row:has(a.chapter_link[alt])"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrlWithoutDomain("/${urlElement.attr("href")}")
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("span").first()?.text()?.let { parseChapterDate(it) } ?: 0
    }

    private fun parseChapterDate(dateAsString: String): Long {
        val m = datePattern.matcher(dateAsString)

        if (m.matches()) {
            val amount = Integer.parseInt(m.group(1))
            val unit = m.group(2)

            return Calendar.getInstance().apply {
                add(dateFields[unit]!!, -amount)
            }.time.time
        } else {
            return 0
        }
    }

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        val document = response.asJsoup()
        val url = response.request().url().toString().substringBeforeLast('/')

        val series = document.select("input[name=series]").first().attr("value")
        val chapter = document.select("input[name=chapter]").first().attr("value")
        val index = document.select("input[name=index]").first().attr("value")

        document.select("select[name=page] > option").forEach {
            pages.add(Page(pages.size, "$url/?series=$series&chapter=$chapter&index=$index&page=${pages.size + 1}"))
        }
        pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
    }

    // Not used, overrides parent.
    override fun pageListParse(document: Document, pages: MutableList<Page>) {
    }

    override fun imageUrlParse(document: Document) = document.select("div > a > img").attr("src")

}