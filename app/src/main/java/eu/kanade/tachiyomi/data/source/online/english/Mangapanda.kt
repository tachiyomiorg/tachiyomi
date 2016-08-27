package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.UrlUtil
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

class Mangapanda(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Mangapanda"

    override val baseUrl = "http://mangapanda.com"

    override val lang: Language get() = EN

    override fun popularMangaInitialUrl() = "$baseUrl/popular"

    override fun popularMangaSelector() = "div#mangaresults > div.mangaresultitem"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("div.manga_name > div > h3 > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun popularMangaNextPageSelector() = "div#sp > a:contains(>)"

    override fun searchMangaInitialUrl(query: String) =
            "$baseUrl/search/?w=$query&rd=0&status=0&order=0&genre=0000000000000000000000000000000000000&p=0"

    override fun searchMangaSelector() = "table#listing > tbody > tr:gt(0)"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = "div#sp > a:contains(>)"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val infoElement = document.select("div#mangaproperties").first().select("table > tbody").first()

        manga.status = infoElement.select("tr:eq(3) > td:eq(1)").first()?.text().orEmpty().let { parseStatus(it) }
        manga.author = infoElement.select("tr:eq(4) > td:eq(1)").first()?.text()
        manga.artist = infoElement.select("tr:eq(5) > td:eq(1)").first()?.text()
        manga.genre = infoElement.select("tr:eq(7) > td:eq(1)").first()?.text()
        manga.description = document.select("div#readmangasum").first()?.text()
        manga.thumbnail_url = document.select("div#mangaimg > img").first()?.attr("src")
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "table#listing tr:gt(0)"

    override fun chapterListParse(response: Response, chapters: MutableList<Chapter>) {
        super.chapterListParse(response, chapters)
        chapters.reverse()
    }

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.url = UrlUtil.getPath(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td:eq(1)").first()?.text()?.let {
            SimpleDateFormat("MM/dd/yyyy").parse(it).time
        } ?: 0
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        val url = document.baseUri().toString()
        document.select("select#pageMenu").first()?.getElementsByTag("option")?.forEach {
            val page = it.attr("value").substringAfterLast('/')
            pages.add(Page(pages.size, "$url/$page"))
        }
        pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
    }

    override fun imageUrlParse(document: Document) = document.getElementById("img").attr("src")

}