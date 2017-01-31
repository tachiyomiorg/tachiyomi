package eu.kanade.tachiyomi.source.online.russian

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

class Mangachan : ParsedHttpSource() {

    override val id: Long = 7

    override val name = "Mangachan"

    override val baseUrl = "http://mangachan.me"

    override val lang = "ru"

    override val supportsLatest = true

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/mostfavorites?offset=${20 * (page - 1)}", headers)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        var pageNum = 1
        when {
            page <  1 -> pageNum = 1
            page >= 1 -> pageNum = page
        }
        val url = if (query.isNotEmpty()) {
            "$baseUrl/?do=search&subaction=search&story=$query&search_start=$pageNum"
        } else {
            val filt = filters.filterIsInstance<Genre>().filter { !it.isIgnored() }
            if (filt.isNotEmpty()) {
                var genres = ""
                filt.forEach { genres += (if (it.isExcluded()) "-" else "") + it.id + '+' }
                "$baseUrl/tags/${genres.dropLast(1)}?offset=${20 * (pageNum - 1)}"
            } else {
                "$baseUrl/?do=search&subaction=search&story=$query&search_start=$pageNum"
            }
        }
        return GET(url, headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/newestch?page=$page")
    }

    override fun popularMangaSelector() = "div.content_row"

    override fun latestUpdatesSelector() = "ul.area_rightNews li"

    override fun searchMangaSelector() = popularMangaSelector()

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("h2 > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a:nth-child(1)").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun searchMangaFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun popularMangaNextPageSelector() = "a:contains(Вперед)"

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaNextPageSelector() = "a:contains(Далее)"

    private fun searchGenresNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        var hasNextPage = false

        val mangas = document.select(searchMangaSelector()).map { element ->
            searchMangaFromElement(element)
        }

        val nextSearchPage = document.select(searchMangaNextPageSelector())
        if (nextSearchPage.isNotEmpty()) {
            val query = document.select("input#searchinput").first().attr("value")
            val pageNum = nextSearchPage.let { selector ->
                val onClick = selector.attr("onclick")
                onClick?.split("""\\d+""")
            }
            nextSearchPage.attr("href", "$baseUrl/?do=search&subaction=search&story=$query&search_start=$pageNum")
            hasNextPage = true
        }

        val nextGenresPage = document.select(searchGenresNextPageSelector())
        if (nextGenresPage.isNotEmpty()) {
            hasNextPage = true
        }

        return MangasPage(mangas, hasNextPage)
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("table.mangatitle").first()
        val descElement = document.select("div#description").first()
        val imgElement = document.select("img#cover").first()

        val manga = SManga.create()
        manga.author = infoElement.select("tr:eq(2) > td:eq(1)").text()
        manga.genre = infoElement.select("tr:eq(5) > td:eq(1)").text()
        manga.status = parseStatus(infoElement.select("tr:eq(4) > td:eq(1)").text())
        manga.description = descElement.textNodes().first().text()
        manga.thumbnail_url = baseUrl + imgElement.attr("src")
        return manga
    }

    private fun parseStatus(element: String): Int {
        when {
            element.contains("перевод завершен") -> return SManga.COMPLETED
            element.contains("перевод продолжается") -> return SManga.ONGOING
            else -> return SManga.UNKNOWN
        }
    }

    override fun chapterListSelector() = "table.table_cha tr:gt(1)"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("div.date").first()?.text()?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it).time
        } ?: 0
        return chapter
    }

    override fun pageListParse(response: Response): List<Page> {
        val html = response.body().string()
        val beginIndex = html.indexOf("fullimg\":[") + 10
        val endIndex = html.indexOf(",]", beginIndex)
        val trimmedHtml = html.substring(beginIndex, endIndex).replace("\"", "")
        val pageUrls = trimmedHtml.split(',')

        return pageUrls.mapIndexed { i, url -> Page(i, "", url) }
    }

    override fun pageListParse(document: Document): List<Page> {
        throw Exception("Not used")
    }

    override fun imageUrlParse(document: Document) = ""

    private class Genre(name: String, val id: String = name.replace(' ', '_')) : Filter.TriState(name)

    /* [...document.querySelectorAll("li.sidetag > a:nth-child(1)")].map((el,i) =>
    *  { const link=el.getAttribute('href');const id=link.substr(6,link.length);
    *  return `Genre("${id.replace("_", " ")}")` }).join(',\n')
    *  on http://mangachan.me/
    */
    override fun getFilterList() = FilterList(
            Genre("18 плюс"),
            Genre("bdsm"),
            Genre("арт"),
            Genre("биография"),
            Genre("боевик"),
            Genre("боевые искусства"),
            Genre("вампиры"),
            Genre("веб"),
            Genre("гарем"),
            Genre("гендерная интрига"),
            Genre("героическое фэнтези"),
            Genre("детектив"),
            Genre("дзёсэй"),
            Genre("додзинси"),
            Genre("драма"),
            Genre("игра"),
            Genre("инцест"),
            Genre("искусство"),
            Genre("история"),
            Genre("киберпанк"),
            Genre("кодомо"),
            Genre("комедия"),
            Genre("литРПГ"),
            Genre("магия"),
            Genre("махо-сёдзё"),
            Genre("меха"),
            Genre("мистика"),
            Genre("музыка"),
            Genre("научная фантастика"),
            Genre("повседневность"),
            Genre("постапокалиптика"),
            Genre("приключения"),
            Genre("психология"),
            Genre("романтика"),
            Genre("самурайский боевик"),
            Genre("сборник"),
            Genre("сверхъестественное"),
            Genre("сказка"),
            Genre("спорт"),
            Genre("супергерои"),
            Genre("сэйнэн"),
            Genre("сёдзё"),
            Genre("сёдзё-ай"),
            Genre("сёнэн"),
            Genre("сёнэн-ай"),
            Genre("тентакли"),
            Genre("трагедия"),
            Genre("триллер"),
            Genre("ужасы"),
            Genre("фантастика"),
            Genre("фурри"),
            Genre("фэнтези"),
            Genre("школа"),
            Genre("эротика"),
            Genre("юри"),
            Genre("яой"),
            Genre("ёнкома")
    )
}