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

class Mintmanga(override val id: Int) : ParsedOnlineSource() {

    override val name = "Mintmanga"

    override val baseUrl = "http://mintmanga.com"

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
    *  on http://mintmanga.com/search
    */
    override fun getFilterList(): List<Filter> = listOf(
            Filter("el_2220", "арт"),
            Filter("el_1353", "бара"),
            Filter("el_1346", "боевик"),
            Filter("el_1334", "боевые искусства"),
            Filter("el_1339", "вампиры"),
            Filter("el_1333", "гарем"),
            Filter("el_1347", "гендерная интрига"),
            Filter("el_1337", "героическое фэнтези"),
            Filter("el_1343", "детектив"),
            Filter("el_1349", "дзёсэй"),
            Filter("el_1332", "додзинси"),
            Filter("el_1310", "драма"),
            Filter("el_5229", "игра"),
            Filter("el_1311", "история"),
            Filter("el_1351", "киберпанк"),
            Filter("el_1328", "комедия"),
            Filter("el_1318", "меха"),
            Filter("el_1324", "мистика"),
            Filter("el_1325", "научная фантастика"),
            Filter("el_1327", "повседневность"),
            Filter("el_1342", "постапокалиптика"),
            Filter("el_1322", "приключения"),
            Filter("el_1335", "психология"),
            Filter("el_1313", "романтика"),
            Filter("el_1316", "самурайский боевик"),
            Filter("el_1350", "сверхъестественное"),
            Filter("el_1314", "сёдзё"),
            Filter("el_1320", "сёдзё-ай"),
            Filter("el_1326", "сёнэн"),
            Filter("el_1330", "сёнэн-ай"),
            Filter("el_1321", "спорт"),
            Filter("el_1329", "сэйнэн"),
            Filter("el_1344", "трагедия"),
            Filter("el_1341", "триллер"),
            Filter("el_1317", "ужасы"),
            Filter("el_1331", "фантастика"),
            Filter("el_1323", "фэнтези"),
            Filter("el_1319", "школа"),
            Filter("el_1340", "эротика"),
            Filter("el_1354", "этти"),
            Filter("el_1315", "юри"),
            Filter("el_1336", "яой")
    )
}