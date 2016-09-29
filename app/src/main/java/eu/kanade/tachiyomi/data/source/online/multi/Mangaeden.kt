package eu.kanade.tachiyomi.data.source.online.multi

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.*

abstract class Mangaeden(context: Context) : ParsedOnlineSource(context) {

    override val name = "MangaEden"

    protected abstract val langcode: String

    override val baseUrl = "http://www.mangaeden.com"

    override fun popularMangaInitialUrl() = "$baseUrl/$langcode/$langcode-directory/"

    override fun popularMangaSelector() = "table#mangaList > tbody > tr"

    override fun popularMangaParse(response: Response, page: MangasPage) {
        super.popularMangaParse(response, page)
    }

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("td:eq(0) > a").let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) : String {
        return "$baseUrl/$langcode/$langcode-directory/?title=$query"
    }

    override fun searchMangaSelector() = "table#mangaList > tbody > tr"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    protected fun getTextNodes(list: List<Node>) : ArrayList<String> {
        val nlist: ArrayList<String> = ArrayList()
        for (element in list) {
            if (element is TextNode) {
                if (!element.text().contains("\n") && !element.text().equals(" ")) {
                    nlist.add(element.text())
                }
            } else if (element is Element){
                if (!element.tagName().equals("br")) {
                    nlist.add(element.text())
                }
            }
        }
        return nlist
    }

    protected abstract fun parseStatus(status: String) : Int

    override fun chapterListSelector() = "table > tbody > tr"

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        val url = document.baseUri().toString()
        document.select("select#pageSelect").first()?.getElementsByTag("option")?.forEach {
            val page = it.attr("value").substringAfterLast('/')
                pages.add(Page(pages.size, "$url/$page"))
            }
        pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
    }

    override fun imageUrlParse(document: Document): String = "http:" + document.select("div#mainImgC > a#nextA > img").attr("src")

}
