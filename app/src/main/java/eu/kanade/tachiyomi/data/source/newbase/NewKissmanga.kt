package eu.kanade.tachiyomi.data.source.newbase

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.get
import eu.kanade.tachiyomi.data.network.post
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class NewKissmanga(context: Context) : ParsedOnlineSource(context) {


    override val name = "Kissmanga"

    val IP = "93.174.95.110"

    override val baseUrl = "http://$IP"

    override val lang: Language get() = EN

    override fun headersBuilder() = super.headersBuilder().add("Host", "kissmanga.com")

    override fun getInitialPopularMangasUrl() = "$baseUrl/MangaList/MostPopular"

    override fun getPopularMangaSelector() = "table.listing tr:gt(1)"

    override fun constructPopularMangaFromElement(element: Element, manga: Manga) {
        element.select("td a:eq(0)").first()?.let {
            manga.setUrl(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun searchMangaRequest(page: MangasPage, query: String): Request {
        if (page.page == 1) {
            page.url = getInitialSearchUrl(query)
        }

        val form = FormBody.Builder().apply {
            add("authorArtist", "")
            add("mangaName", query)
            add("status", "")
            add("genres", "")
        }.build()

        return post(page.url, headers, form)
    }

    override fun getNextPopularPageSelector() = "li > a:contains(› Next)"

    override fun getSearchMangaSelector() = getPopularMangaSelector()

    override fun constructSearchMangaFromElement(element: Element, manga: Manga) {
        constructPopularMangaFromElement(element, manga)
    }

    override fun getNextSearchPageSelector() = null

    override fun getInitialSearchUrl(query: String) = "$baseUrl/AdvanceSearch"

    override fun constructMangaFromDocument(document: Document, manga: Manga) {
        val infoElement = document.select("div.barContent").first()

        manga.title = infoElement.select("a.bigChar").first().text()
        manga.author = infoElement.select("p:has(span:contains(Author:)) > a").first()?.text()
        manga.genre = infoElement.select("p:has(span:contains(Genres:)) > *:gt(0)").text()
        manga.description = infoElement.select("p:has(span:contains(Summary:)) ~ p").text()
        manga.status = parseStatus(infoElement.select("p:has(span:contains(Status:))").first()?.text())
        manga.thumbnail_url = document.select(".rightBox:eq(0) img").first()?.attr("src")?.let {
            Uri.parse(it).buildUpon().authority(IP).toString();
        }
    }

    fun parseStatus(status: String?): Int {
        if (status != null) {
            when {
                status.contains("Ongoing") -> return Manga.ONGOING
                status.contains("Completed") -> return Manga.COMPLETED
            }
        }
        return Manga.UNKNOWN
    }

    override fun getChapterListSelector() = "table.listing tr:gt(1)"

    override fun constructChapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrl(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td:eq(1)").first()?.text()?.let {
            SimpleDateFormat("MM/dd/yyyy").parse(it).time
        } ?: 0
    }

    override fun pageListRequest(chapter: Chapter) = post(baseUrl + chapter.url, headers)

    override fun parseHtmlToPages(response: Response, html: String, pages: MutableList<Page>) {
        val p = Pattern.compile("lstImages.push\\(\"(.+?)\"")
        val m = p.matcher(html)

        var i = 0
        while (m.find()) {
            Page(i++, "").apply {
                imageUrl = m.group(1)
                pages.add(this)
            }
        }
    }

    // Not used
    override fun parseDocumentToPages(document: Document, pages: MutableList<Page>) {}

    override fun imageUrlRequest(page: Page) = get(page.url)

    override fun parseDocumentToImageUrl(document: Document) = ""

}