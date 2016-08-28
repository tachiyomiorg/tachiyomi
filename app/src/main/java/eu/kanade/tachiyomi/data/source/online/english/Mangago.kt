/*
 * This file is part of Kensaku.
 *
 * Kensaku is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kensaku is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kensaku.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context
import android.util.Log
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
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
            manga.thumbnail_url = element.select("a.thm-effect > img").attr("src")
        }
    }

    override fun popularMangaParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()

        for (element in document.select(popularMangaSelector())) {
            Manga.create(id).apply {
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }
        popularMangaNextPageSelector().let {
            page.nextPageUrl = document.select(it).html().substringAfter("href=\"").substringBefore("\" class")//WTF this is necessary IDK
        }
    }

    override fun popularMangaNextPageSelector() = "div.pagination > div > ol > li.current + li"

    override fun searchMangaInitialUrl(query: String) =
            "$baseUrl/r/l_search/?page=1&name=$query"

    override fun searchMangaSelector() = "ul#search_list > li"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.select("div.box").let {
            manga.setUrlWithoutDomain(it.select("div.row-1 h2 > a").attr("href"))
            manga.title = it.select("a.thm-effect").attr("title")
            manga.thumbnail_url = element.select("a.thm-effect > img").attr("src")
        }
    }

    override fun searchMangaParse(response: Response, page: MangasPage, query: String) {
        val document = response.asJsoup()
        val pSel = "select option"
        val p: String = document.select("div.pagination").attr("total")
        val pageCount = if (p.isEmpty()) 0 else p.toInt()

        for (element in document.select(searchMangaSelector())) {
            Manga.create(id).apply {
                searchMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }
        if (pageCount > 1) {
            pSel.let { selector ->
                val i = page.url.toString().substringAfter("page=").substringBefore('&').toInt() + 1
                page.nextPageUrl = document.select(selector).first().attr("value").toString().replace("page=1", "page=$i")
            }
        }
    }

    override fun searchMangaNextPageSelector() = "div.pagination > div > ol > li.current + li"

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
