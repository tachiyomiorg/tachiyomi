package eu.kanade.tachiyomi.data.source.online.russian

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Readmanga(override val id: Int) : ParsedOnlineSource() {

    override val name = "Readmanga"

    override val baseUrl = "http://readmanga.me"

    override val lang = "ru"

    override val supportsLatest = true

    override fun popularMangaInitialUrl() = "$baseUrl/list?sortType=rate"

    override fun latestUpdatesInitialUrl() = "$baseUrl/list?sortType=updated"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/search?q=$query&${filters.map { it.id + "=in" }.joinToString("&")}"

    override fun popularMangaSelector() = "div.desc"

    override fun latestUpdatesSelector() = "div.desc"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("h3 > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.attr("title")
        }
    }

    override fun latestUpdatesFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun popularMangaNextPageSelector() = "a.nextLink"

    override fun latestUpdatesNextPageSelector() = "a.nextLink"

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    // max 200 results
    override fun searchMangaNextPageSelector() = null

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val infoElement = document.select("div.leftContent").first()

        manga.author = infoElement.select("span.elem_author").first()?.text()
        manga.genre = infoElement.select("span.elem_genre").text().replace(" ,", ",")
        manga.description = infoElement.select("div.manga-description").text()
        manga.status = parseStatus(infoElement.html())
        manga.thumbnail_url = infoElement.select("img").attr("data-full")
    }

    private fun parseStatus(element: String): Int {
        when {
            element.contains("<h3>Запрещена публикация произведения по копирайту</h3>") -> return Manga.LICENSED
            element.contains("<h1 class=\"names\"> Сингл") || element.contains("<b>Перевод:</b> завершен") -> return Manga.COMPLETED
            element.contains("<b>Перевод:</b> продолжается") -> return Manga.ONGOING
            else -> return Manga.UNKNOWN
        }
    }

    override fun chapterListSelector() = "div.chapters-link tbody tr"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href") + "?mature=1")
        chapter.name = urlElement.text().replace(" новое", "")
        chapter.date_upload = element.select("td:eq(1)").first()?.text()?.let {
            SimpleDateFormat("dd/MM/yy", Locale.US).parse(it).time
        } ?: 0
    }

    override fun prepareNewChapter(chapter: Chapter, manga: Manga) {
        chapter.chapter_number = -2f
    }

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        val html = response.body().string()
        val beginIndex = html.indexOf("rm_h.init( [")
        val endIndex = html.indexOf("], 0, false);", beginIndex)
        val trimmedHtml = html.substring(beginIndex, endIndex)

        val p = Pattern.compile("'.+?','.+?',\".+?\"")
        val m = p.matcher(trimmedHtml)

        var i = 0
        while (m.find()) {
            val urlParts = m.group().replace("[\"\']+".toRegex(), "").split(',')
            pages.add(Page(i++, "", urlParts[1] + urlParts[0] + urlParts[2]))
        }
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) { }

    override fun imageUrlParse(document: Document) = ""

    /* [...document.querySelectorAll("tr.advanced_option:nth-child(1) > td:nth-child(3) span.js-link")].map((el,i) => {
    *  const onClick=el.getAttribute('onclick');const id=onClick.substr(31,onClick.length-33);
    *  return `Filter("${id}", "${el.textContent.trim()}")` }).join(',\n')
    *  on http://readmanga.me/search
    */
    override fun getFilterList(): List<Filter> = listOf(
            Filter("el_5685", "арт"),
            Filter("el_2155", "боевик"),
            Filter("el_2143", "боевые искусства"),
            Filter("el_2148", "вампиры"),
            Filter("el_2142", "гарем"),
            Filter("el_2156", "гендерная интрига"),
            Filter("el_2146", "героическое фэнтези"),
            Filter("el_2152", "детектив"),
            Filter("el_2158", "дзёсэй"),
            Filter("el_2141", "додзинси"),
            Filter("el_2118", "драма"),
            Filter("el_2154", "игра"),
            Filter("el_2119", "история"),
            Filter("el_8032", "киберпанк"),
            Filter("el_2137", "кодомо"),
            Filter("el_2136", "комедия"),
            Filter("el_2147", "махо-сёдзё"),
            Filter("el_2126", "меха"),
            Filter("el_2132", "мистика"),
            Filter("el_2133", "научная фантастика"),
            Filter("el_2135", "повседневность"),
            Filter("el_2151", "постапокалиптика"),
            Filter("el_2130", "приключения"),
            Filter("el_2144", "психология"),
            Filter("el_2121", "романтика"),
            Filter("el_2124", "самурайский боевик"),
            Filter("el_2159", "сверхъестественное"),
            Filter("el_2122", "сёдзё"),
            Filter("el_2128", "сёдзё-ай"),
            Filter("el_2134", "сёнэн"),
            Filter("el_2139", "сёнэн-ай"),
            Filter("el_2129", "спорт"),
            Filter("el_2138", "сэйнэн"),
            Filter("el_2153", "трагедия"),
            Filter("el_2150", "триллер"),
            Filter("el_2125", "ужасы"),
            Filter("el_2140", "фантастика"),
            Filter("el_2131", "фэнтези"),
            Filter("el_2127", "школа"),
            Filter("el_2149", "этти"),
            Filter("el_2123", "юри")
    )
}