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

class Mangachan(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Mangachan"

    override val baseUrl = "http://mangachan.me"

    override val lang: Language get() = RU

    override fun popularMangaInitialUrl() = "$baseUrl/mostfavorites"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) = "$baseUrl/?do=search&subaction=search&story=$query"

    override fun popularMangaSelector() = "div.content_row"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("h2 > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun popularMangaNextPageSelector() = "a:contains(Вперед)"

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

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

        for ((i, url) in pageUrls.withIndex()) {
            pages.add(Page(i, "", url))
        }
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) { }

    override fun imageUrlParse(document: Document) = ""
}