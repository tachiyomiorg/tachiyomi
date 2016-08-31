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
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.UrlUtil
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat

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

    override fun searchMangaInitialUrl(query: String, filters: List<OnlineSource.Filter>) =
            "$baseUrl/search/?w=$query&rd=0&status=0&order=0&genre=0000000000000000000000000000000000000&p=0"

    override fun searchMangaSelector() = "table#listing > tbody > tr:gt(0)"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = "div#sp > a:contains(>)"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val infoElement = document.select("div#mangaproperties").first().select("table > tbody").first()

        manga.thumbnail_url = document.select("div#mangaimg > img").first()?.attr("src")
        manga.status = infoElement.select("tr:eq(3) > td:eq(1)").first()?.text().orEmpty().let { parseStatus(it) }
        manga.author = infoElement.select("tr:eq(4) > td:eq(1)").first()?.text()
        manga.artist = infoElement.select("tr:eq(5) > td:eq(1)").first()?.text()
        manga.genre = infoElement.select("tr:eq(7) > td:eq(1)").first()?.text()
        manga.description = document.select("div#readmangasum").first()?.text()
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

    override fun imageUrlParse(document: Document): String = document.getElementById("img").attr("src")

    override fun getFilterList(): List<Filter> = listOf(
            Filter("1", "Action"),
            Filter("1", "Adventure"),
            Filter("1", "Comedy"),
            Filter("1", "Demons"),
            Filter("1", "Drama"),
            Filter("1", "Ecchi"),
            Filter("1", "Fantasy"),
            Filter("1", "Gender Bender"),
            Filter("1", "Harem"),
            Filter("1", "Historical"),
            Filter("1", "Horror"),
            Filter("1", "Josei"),
            Filter("1", "Magic"),
            Filter("1", "Martial Arts"),
            Filter("1", "Mature"),
            Filter("1", "Mecha"),
            Filter("1", "Military"),
            Filter("1", "Mystery"),
            Filter("1", "One shot"),
            Filter("1", "Psychological"),
            Filter("1", "Romance"),
            Filter("1", "School Life"),
            Filter("1", "Sci-fi"),
            Filter("1", "Seinen"),
            Filter("1", "Shotacon"),
            Filter("1", "Shoujo"),
            Filter("1", "Shoujo Ai"),
            Filter("1", "Shounen"),
            Filter("1", "Shounen Ai"),
            Filter("1", "Slice of Life"),
            Filter("1", "Smut"),
            Filter("1", "Sports"),
            Filter("1", "Supernatural"),
            Filter("1", "Tragedy"),
            Filter("1", "Vampire"),
            Filter("1", "Yaoi"),
            Filter("1", "Yuri")
    )
}