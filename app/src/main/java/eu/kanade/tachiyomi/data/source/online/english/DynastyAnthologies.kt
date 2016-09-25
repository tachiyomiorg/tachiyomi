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
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.json.JSONArray
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class DynastyAnthologies(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Dynasty-Anthologies"

    override val baseUrl = "http://dynasty-scans.com"

    override val lang: Language get() = EN

    override fun popularMangaInitialUrl() = "$baseUrl/anthologies?view=cover"

    override fun popularMangaSelector() = "ul.thumbnails > li.span2"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.let {
            manga.setUrlWithoutDomain(it.select("a").attr("href"))
            manga.title = it.select("div.caption").text()
            manga.thumbnail_url = baseUrl + it.select("img").attr("src")
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
    }

    override fun popularMangaNextPageSelector() = ""

    override fun searchMangaInitialUrl(query: String, filters: List<OnlineSource.Filter>) =
            "$baseUrl/search?q=$query&classes[]=Anthology&classes[]=Issue&classes[]=Series&sort="

    override fun searchMangaSelector() = "a.name"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun searchMangaNextPageSelector() = "div.pagination > ul > li.active + li > a"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val ielement = document.select("div#main").first()

        if (manga.thumbnail_url == null) manga.thumbnail_url = baseUrl + document.select("div.span2 > img").attr("src")
        manga.status = ielement.select("h2 > small").text().orEmpty().let { parseStatus(it) }
        manga.genre = document.select("div.tag-tags > a").text() + ","
        manga.description = ielement.select("div.description").text()
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "div.span10 > dl.chapter-list > dd"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val nodes = element.childNodes()

        chapter.setUrlWithoutDomain((nodes[1] as Element).attr("href"))
        chapter.name = (nodes[1] as Element).text() + " by " + (nodes[3] as Element).text()
        chapter.date_upload = (nodes[7] as Element).text().let {
            SimpleDateFormat("MMM dd yy").parse(it.substringAfter("released ").replace("\'", "")).time
        }
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        try {
            val script = document.select("script").last()
            val p = Pattern.compile("(?s)(pages)\\s??=\\s??\\[(.*?)\\]")
            val m = p.matcher(script.html())
            var imageUrls = JSONArray()
            while (m.find())
                imageUrls = JSONArray("[" + m.group(2) + "]")

            for (i in 0..imageUrls.length() - 1) {
                val jsonObject = imageUrls.getJSONObject(i)
                val image = baseUrl + jsonObject.get("image")
                pages.add(Page(pages.size, "", image))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun imageUrlParse(document: Document): String = null!!
}
