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

class DynastyDoujins(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Dynasty-Doujins"

    override val baseUrl = "http://dynasty-scans.com"

    override val lang: Language get() = EN

    override fun popularMangaInitialUrl() = "$baseUrl/doujins?view=cover"

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
        manga.status = Manga.UNKNOWN
        manga.genre = document.select("div.tag-tags > a").text()
    }

    override fun chapterListSelector() = "div.span9 > dl.chapter-list > dd"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val nodes = element.childNodes()

        chapter.setUrlWithoutDomain((nodes[1] as Element).attr("href"))
        chapter.name = (nodes[1] as Element).text() + " by " + (nodes[3] as Element).text()
        chapter.date_upload = (nodes[5] as Element).text().let {
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
