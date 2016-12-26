package eu.kanade.tachiyomi.data.source.online.russian

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

class Mangachan(override val id: Int) : ParsedOnlineSource() {

    override val name = "Mangachan"

    override val baseUrl = "http://mangachan.me"

    override val lang = "ru"

    override val supportsLatest = true

    override fun popularMangaInitialUrl() = "$baseUrl/mostfavorites"

    override fun latestUpdatesInitialUrl() = "$baseUrl/newestch"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>): String {
        if (query.isNotEmpty()) {
            return "$baseUrl/?do=search&subaction=search&story=$query"
        } else if (filters.isNotEmpty()) {
            var genres = ""
            filters.forEach { genres = genres + it.name + '+' }
            return "$baseUrl/tags/${genres.dropLast(1)}"
        } else {
            return "$baseUrl/?do=search&subaction=search&story=$query"
        }
    }

    override fun popularMangaSelector() = "div.content_row"

    override fun latestUpdatesSelector() = "ul.area_rightNews li"

    override fun searchMangaSelector() = popularMangaSelector()

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("h2 > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun latestUpdatesFromElement(element: Element, manga: Manga) {
        element.select("a:nth-child(1)").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun popularMangaNextPageSelector() = "a:contains(Вперед)"

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaNextPageSelector() = "a:contains(Далее)"

    private fun searchGenresNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaParse(response: Response, page: MangasPage, query: String, filters: List<Filter>) {
        val document = response.asJsoup()
        for (element in document.select(searchMangaSelector())) {
            Manga.create(id).apply {
                searchMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        searchMangaNextPageSelector().let { selector ->
            if (page.nextPageUrl.isNullOrEmpty() && filters.isEmpty()) {
                val onClick = document.select(selector).first()?.attr("onclick")
                val pageNum = onClick?.substring(23, onClick.indexOf("); return(false)"))
                page.nextPageUrl = searchMangaInitialUrl(query, emptyList()) + "&search_start=" + pageNum
            }
        }

        searchGenresNextPageSelector().let { selector ->
            if (page.nextPageUrl.isNullOrEmpty() && filters.isNotEmpty()) {
                val url = document.select(selector).first()?.attr("href")
                page.nextPageUrl = searchMangaInitialUrl(query, filters) + url
            }
        }
    }

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val infoElement = document.select("table.mangatitle").first()
        val descElement = document.select("div#description").first()
        val imgElement = document.select("img#cover").first()

        manga.author = infoElement.select("tr:eq(2) > td:eq(1)").text()
        manga.genre = infoElement.select("tr:eq(5) > td:eq(1)").text()
        manga.status = parseStatus(infoElement.select("tr:eq(4) > td:eq(1)").text())
        manga.description = descElement.textNodes().first().text()
        manga.thumbnail_url = baseUrl + imgElement.attr("src")
    }

    private fun parseStatus(element: String): Int {
        when {
            element.contains("перевод завершен") -> return Manga.COMPLETED
            element.contains("перевод продолжается") -> return Manga.ONGOING
            else -> return Manga.UNKNOWN
        }
    }

    override fun chapterListSelector() = "table.table_cha tr:gt(1)"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("div.date").first()?.text()?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it).time
        } ?: 0
    }

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        val html = response.body().string()
        val beginIndex = html.indexOf("fullimg\":[") + 10
        val endIndex = html.indexOf(",]", beginIndex)
        val trimmedHtml = html.substring(beginIndex, endIndex).replace("\"", "")
        val pageUrls = trimmedHtml.split(',')

        pageUrls.mapIndexedTo(pages) { i, url -> Page(i, "", url) }
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) { }

    override fun imageUrlParse(document: Document) = ""

    /* [...document.querySelectorAll("li.sidetag > a:nth-child(1)")].map((el,i) =>
    *  { const link=el.getAttribute('href');const id=link.substr(6,link.length);
    *  return `Filter("${id}", "${id}")` }).join(',\n')
    *  on http://mangachan.me/
    */
    override fun getFilterList(): List<Filter> = listOf(
            Filter("18_плюс", "18_плюс"),
            Filter("bdsm", "bdsm"),
            Filter("арт", "арт"),
            Filter("биография", "биография"),
            Filter("боевик", "боевик"),
            Filter("боевые_искусства", "боевые_искусства"),
            Filter("вампиры", "вампиры"),
            Filter("веб", "веб"),
            Filter("гарем", "гарем"),
            Filter("гендерная_интрига", "гендерная_интрига"),
            Filter("героическое_фэнтези", "героическое_фэнтези"),
            Filter("детектив", "детектив"),
            Filter("дзёсэй", "дзёсэй"),
            Filter("додзинси", "додзинси"),
            Filter("драма", "драма"),
            Filter("игра", "игра"),
            Filter("инцест", "инцест"),
            Filter("искусство", "искусство"),
            Filter("история", "история"),
            Filter("киберпанк", "киберпанк"),
            Filter("кодомо", "кодомо"),
            Filter("комедия", "комедия"),
            Filter("литРПГ", "литРПГ"),
            Filter("махо-сёдзё", "махо-сёдзё"),
            Filter("меха", "меха"),
            Filter("мистика", "мистика"),
            Filter("музыка", "музыка"),
            Filter("научная_фантастика", "научная_фантастика"),
            Filter("повседневность", "повседневность"),
            Filter("постапокалиптика", "постапокалиптика"),
            Filter("приключения", "приключения"),
            Filter("психология", "психология"),
            Filter("романтика", "романтика"),
            Filter("самурайский_боевик", "самурайский_боевик"),
            Filter("сборник", "сборник"),
            Filter("сверхъестественное", "сверхъестественное"),
            Filter("сказка", "сказка"),
            Filter("спорт", "спорт"),
            Filter("супергерои", "супергерои"),
            Filter("сэйнэн", "сэйнэн"),
            Filter("сёдзё", "сёдзё"),
            Filter("сёдзё-ай", "сёдзё-ай"),
            Filter("сёнэн", "сёнэн"),
            Filter("сёнэн-ай", "сёнэн-ай"),
            Filter("тентакли", "тентакли"),
            Filter("трагедия", "трагедия"),
            Filter("триллер", "триллер"),
            Filter("ужасы", "ужасы"),
            Filter("фантастика", "фантастика"),
            Filter("фурри", "фурри"),
            Filter("фэнтези", "фэнтези"),
            Filter("школа", "школа"),
            Filter("эротика", "эротика"),
            Filter("юри", "юри"),
            Filter("яой", "яой"),
            Filter("ёнкома", "ёнкома")
    )
}