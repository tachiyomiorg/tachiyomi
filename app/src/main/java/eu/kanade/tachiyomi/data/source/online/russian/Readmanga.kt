package eu.kanade.tachiyomi.data.source.online.russian

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.RU
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Readmanga(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Readmanga"

    override val baseUrl = "http://readmanga.me"

    override val lang: Language get() = RU

    override fun popularMangaInitialUrl() = "$baseUrl/list?sortType=rate"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) = "$baseUrl/search?q=$query"

    override fun popularMangaSelector() = "div.desc"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("h3 > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.attr("title")
        }
    }

    override fun popularMangaNextPageSelector() = "a.nextLink"

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

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

    override fun parseChapterNumber(chapter: Chapter) {
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
}