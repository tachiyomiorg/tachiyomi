package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.get
import eu.kanade.tachiyomi.data.network.post
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.base.ParsedOnlineSource
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class Kissmanga(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Kissmanga"

    override val baseUrl = "http://kissmanga.com"

    override val lang: Language get() = EN

    override val client: OkHttpClient get() = network.cloudflareClient

    override fun popularMangaInitialUrl() = "$baseUrl/MangaList/MostPopular"

    override fun popularMangaSelector() = "table.listing tr:gt(1)"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("td a:eq(0)").first().let {
            manga.setUrl(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun searchMangaRequest(page: MangasPage, query: String): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query)
        }

        val form = FormBody.Builder().apply {
            add("authorArtist", "")
            add("mangaName", query)
            add("status", "")
            add("genres", "")
        }.build()

        return post(page.url, headers, form)
    }

    override fun popularMangaNextPageSelector() = "li > a:contains(› Next)"

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = null

    override fun searchMangaInitialUrl(query: String) = "$baseUrl/AdvanceSearch"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val infoElement = document.select("div.barContent").first()

        manga.author = infoElement.select("p:has(span:contains(Author:)) > a").first()?.text()
        manga.genre = infoElement.select("p:has(span:contains(Genres:)) > *:gt(0)").text()
        manga.description = infoElement.select("p:has(span:contains(Summary:)) ~ p").text()
        manga.status = parseStatus(infoElement.select("p:has(span:contains(Status:))").first()?.text())
        manga.thumbnail_url = document.select(".rightBox:eq(0) img").first()?.attr("src")
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

    override fun chapterListSelector() = "table.listing tr:gt(1)"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrl(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("td:eq(1)").first()?.text()?.let {
            SimpleDateFormat("MM/dd/yyyy").parse(it).time
        } ?: 0
    }

    override fun pageListRequest(chapter: Chapter) = post(baseUrl + chapter.url, headers)

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        val p = Pattern.compile("lstImages.push\\(\"(.+?)\"")
        val m = p.matcher(response.body().string())

        var i = 0
        while (m.find()) {
            pages.add(Page(i++, "", m.group(1)))
        }
    }

    // Not used
    override fun pageListParse(document: Document, pages: MutableList<Page>) {}

    override fun imageUrlRequest(page: Page) = get(page.url)

    override fun imageUrlParse(document: Document) = ""

}