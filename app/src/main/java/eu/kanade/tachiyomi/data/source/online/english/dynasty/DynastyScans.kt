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
package eu.kanade.tachiyomi.data.source.online.english.dynasty

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
import okhttp3.*
import org.json.JSONArray
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

abstract class DynastyScans(context: Context) : ParsedOnlineSource(context) {

    override val baseUrl = "http://dynasty-scans.com"

    override val lang: Language get() = EN

    var parent: List<Node> = ArrayList()

    var list = InternalList(ArrayList(), "")

    var imgList = InternalList(ArrayList(), "")

    var _valid: Validate = Validate(false, -1)

    override fun popularMangaSelector() = "ul.thumbnails > li.span2"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        manga.setUrlWithoutDomain(element.select("a").attr("href"))
        manga.title = element.select("div.caption").text()
    }

    override fun popularMangaParse(response: Response, page: MangasPage) {
        for (element in response.asJsoup().select(popularMangaSelector())) {
            Manga.create(id).apply {
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }
    }

    override fun searchMangaSelector() = "a.name"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
            manga.setUrlWithoutDomain(element.attr("href"))
            manga.title = element.text()
    }

    override fun searchMangaNextPageSelector() = "div.pagination > ul > li.active + li > a"

    private fun buildListfromResponse(): List<Node> {
        return client.newCall(Request.Builder().headers(headers)
                .url(popularMangaInitialUrl()).build()).execute().asJsoup()
                .select("div#main").filter { it.hasText() }.first().childNodes()
    }

    protected fun parseThumbnail(manga: Manga) {
        if (_valid.isManga == false) Log.i("Debug_Thumbnail", manga.url + " ,, " + list[_valid.pos])
        if (_valid.isManga) manga.thumbnail_url = baseUrl + imgList[_valid.pos].substringBefore('?')
    }

    protected fun parseHeader(document: Document, manga: Manga) : Boolean {
        val elements = document.select("div.tags > h2.tag-title").first().getElementsByTag("a")

        if (!elements.isEmpty()) {
            if (elements.lastIndex == 0) {
                manga.author = elements[0].text()
            } else {
                manga.artist = elements[0].text()
                manga.author = elements[1].text()
            }
            manga.status = document.select("div.tags > h2.tag-title > small").text().let {
                when {
                    it.contains("Ongoing") -> Manga.ONGOING
                    it.contains("Completed") -> Manga.COMPLETED
                    else -> Manga.UNKNOWN
                }
            }
            return true
        }
        return false
    }

    protected fun parseGenres(document: Document, manga: Manga) {
        manga.genre = ""
        val glist = document.select("div.tags > div.tag-tags").first().getElementsByTag("a")
        if (!glist.isEmpty()) {
            for (g in glist) {
                val s = g.text()
                manga.genre += if (glist.last().equals(g)) s else "$s, "
            }
        }
    }

    protected fun parseDescription(document: Document, manga: Manga) {
        Log.i("Debug_", "parseDescripion(...) has executed")
        manga.description = document.select("div.tags > div.row div.description").text()
    }

    private fun getValid(manga: Manga): Validate {
        if (parent.isEmpty()) parent = buildListfromResponse()
        if (list.isEmpty()) list = InternalList(parent, "href")
        if (imgList.isEmpty()) imgList = InternalList(parent, "src")
        val pos = list.indexOf(manga.url)
        return Validate((pos > -1), pos)
    }

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        _valid = getValid(manga)
        Log.i("Debug_Valid", _valid.isManga.toString() + " .. " + _valid.pos)
    }

    override fun chapterListSelector() = "div.span10 > dl.chapter-list > dd"

    override fun chapterListParse(response: Response, chapters: MutableList<Chapter>) {
        super.chapterListParse(response, chapters)
        chapters.reverse()
    }

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val nodes = InternalList(element.childNodes(), "text")

        chapter.setUrlWithoutDomain(element.select("a.name").attr("href"))
        chapter.name = nodes[0]
        if (nodes.contains(" by ")) {
            chapter.name += " by ${nodes[nodes.indexOf(" by ") + 1]}"
            if (nodes.contains(" and ")) {
                chapter.name += " and ${nodes[nodes.indexOf(" and ") + 1]}"
            }
        }
        chapter.date_upload = nodes[nodes.indexOf("released")].let {
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

    class InternalList : ArrayList<String> {

        constructor(nodes: List<Node>, type: String) {
            if (type.equals("text")) {
                for (node in nodes) {
                    if (node is TextNode) {
                        if (!node.text().equals(" ") && !node.text().contains("\n"))
                            this.add(node.text())
                    } else if (node is Element) this.add(node.text())
                }
            }
            if (type.equals("src")) {
                for (node in nodes) {
                    if (node is Element && node.hasClass("thumbnails")) {
                        for (n in node.childNodes()) {
                            if (n is Element && n.hasClass("span2")) {
                                this.add(n.child(0).child(0).attr(type))
                            }
                        }
                    }
                }
            }
            if (type.equals("href")) {
                for (node in nodes) {
                    if (node is Element && node.hasClass("thumbnails")) {
                        for (n in node.childNodes()) {
                            if (n is Element && n.hasClass("span2")) {
                                this.add(n.child(0).attr(type))
                            }
                        }
                    }
                }
            }
        }

        override fun indexOf(partial: String): Int {
            for (i in 0 .. this.lastIndex) {
                if (this[i].contains(partial)) return i
            }
            return -1
        }

        fun getItem(partial: String): String {
            for (i in 0 .. this.lastIndex) {
                if (super.get(i).contains(partial)) return super.get(i)
            }
            return ""
        }
    }

    class Validate(_isManga: Boolean, _pos: Int) {
        val isManga = _isManga
        val pos = _pos
    }

    override fun popularMangaNextPageSelector() = ""

    override fun imageUrlParse(document: Document): String = null!!

}