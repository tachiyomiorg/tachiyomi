package eu.kanade.tachiyomi.data.source.online.multi

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat

abstract class Ninemanga(context: Context) : ParsedOnlineSource(context) {

    override val name = "Ninemanga"

    var index: Int = 2

    override fun popularMangaInitialUrl() = "$baseUrl/category/"

    override fun popularMangaSelector() = "div.leftbox > ul.direlist > li"

    override fun popularMangaParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(popularMangaSelector())) {
            Manga.create(id).apply {
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }
        page.nextPageUrl = "${popularMangaInitialUrl()}index_${index++}.html"
    }

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("dl.bookinfo").let {
            val href = it.select("dt > a").attr("href") + "?waring=1"
            manga.setUrlWithoutDomain(href)
            manga.title = element.select("dd > a.bookname").text()
        }
    }

    override fun popularMangaNextPageSelector() = ""

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/search/?name_sel=&wd=$query&author_sel=&author=&artist_sel=&artist=&category_id=&out_category_id=&completed_series=&page=1.html"

    override fun searchMangaSelector() = "div.leftbox > ul.direlist > li"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = "ul.pagelist > li > a:contains(>>)"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val ielement = document.select("div.mainbox > div.manga > div.bookintro")
        val infoElement = ielement.select("ul.message")

        manga.thumbnail_url = ielement.select("img").attr("src")
        manga.genre = infoElement.select("li[itemprop=genre]").text().substringAfter(':')
        manga.author = infoElement.select("li > a[itemprop=author]").text()
        manga.status = infoElement.select("li > a.red").text().orEmpty().let { parseStatus(it) }
        manga.description = ielement.select("p[itemprop=description]").text().substringAfter(':')
    }

    protected abstract fun parseStatus(status: String) : Int

    override fun chapterListSelector() = "div.chapterbox > div.silde > ul > li"

    override fun chapterFromElement(element: Element, chapter: Chapter) {

        val urlElement = element.select("a.chapter_list_a").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.attr("title")
        chapter.date_upload = element.select("span").first()?.text()?.let {
            SimpleDateFormat("MMM dd, yyyy").parse(it).time
        } ?: 0
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        val url = document.baseUri().toString()
        document.select("div.changepage > select#page").first()?.getElementsByTag("option")?.forEach {
            val page = it.attr("value").substringAfterLast('/')
            pages.add(Page(pages.size, "$url/$page"))
        }
        pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
    }

    override fun imageUrlParse(document: Document): String = document.select("div.pic_box > img").attr("src")

}
