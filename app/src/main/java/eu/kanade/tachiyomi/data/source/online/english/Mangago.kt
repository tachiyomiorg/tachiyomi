package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

class Mangago(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Mangago"

    override val baseUrl = "http://www.mangago.me"

    override val lang: Language get() = EN

    override fun popularMangaInitialUrl() = "$baseUrl/genre/all/1/?f=1&o=1&sortby=comment_count&e="

    override fun popularMangaSelector() = "div > ul.pic_list > li.updatesli"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.let {
            manga.setUrlWithoutDomain(it.select("h3.title > a").attr("href"))
            manga.title = it.select("h3.title > a").text()
            manga.thumbnail_url = it.select("a.thm-effect > img").attr("src")
        }
    }

    override fun popularMangaNextPageSelector() = "div.pagination > div > ol > li.current + li > a"

    override fun searchMangaInitialUrl(query: String, filters: List<OnlineSource.Filter>) =
            "$baseUrl/r/l_search/?page=1&name=$query"

    override fun searchMangaSelector() = "ul#search_list > li"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.select("div.box").let {
            manga.setUrlWithoutDomain(it.select("div.row-1 h2 > a").attr("href"))
            manga.title = it.select("a.thm-effect").attr("title")
            manga.thumbnail_url = it.select("a.thm-effect > img").attr("src")
        }
    }

    override fun searchMangaNextPageSelector() = "div.pagination > div > ol > li.current + li > a"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val ielement = document.select("div#information").first()
        val infoElement = ielement.select("div.manga_right > table.left > tbody").first()

        manga.status = infoElement.select("tr:eq(0) > td > span").text().orEmpty().let { parseStatus(it) }
        manga.author = infoElement.select("tr:eq(1) > td > a").text()
        manga.artist = manga.author
        manga.genre = infoElement.select("tr:eq(2) > td > a").text()
        manga.description = ielement.select("div.manga_summary").text()
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "table#chapter_table > tbody > tr"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("td > h4 > a")

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td:eq(1)").first()?.text()?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).parse(it).time
        } ?: 0
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        document.select("div.recom > p").last()?.getElementsByTag("a")?.forEach {
            pages.add(Page(pages.size, it.attr("href")))
        }
    }

    override fun imageUrlParse(document: Document): String = document.getElementById("page1").attr("src")

}
