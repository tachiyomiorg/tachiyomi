package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.POST
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*

class Readmangatoday(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "ReadMangaToday"

    override val baseUrl = "http://www.readmanga.today"

    override val lang: Language get() = EN

    override val client: OkHttpClient get() = network.cloudflareClient

    override fun popularMangaInitialUrl() = "$baseUrl/hot-manga/"

    override fun popularMangaSelector() = "div.hot-manga > div.style-list > div.box"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("div.title > h2 > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.attr("title")
        }
    }

    override fun popularMangaNextPageSelector() = "div.hot-manga > ul.pagination > li > a:contains(»)"

    override fun searchMangaInitialUrl(query: String) =
            "$baseUrl/search"


    override fun searchMangaRequest(page: MangasPage, query: String, filters: List<Source.Filter>): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query)
        }

        var builder = okhttp3.FormBody.Builder()
        builder.add("query", query)

        return POST(page.url, headers, builder.build())
    }

    override fun searchMangaSelector() = "div.content-list > div.style-list > div.box"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.select("div.title > h2 > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.attr("title")
        }
    }

    override fun searchMangaNextPageSelector() = "div.next-page > a.next"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val detailElement = document.select("div.movie-meta").first()

        manga.author = document.select("ul.cast-list li.director > ul a").first()?.text()
        manga.artist = document.select("ul.cast-list li:not(.director) > ul a").first()?.text()
        manga.genre = detailElement.select("dl.dl-horizontal > dd:eq(5)").first()?.text()
        manga.description = detailElement.select("li.movie-detail").first()?.text()
        manga.status = detailElement.select("dl.dl-horizontal > dd:eq(3)").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = detailElement.select("img.img-responsive").first()?.attr("src")
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "ul.chp_lst > li"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.select("span.val").text()
        chapter.date_upload = element.select("span.dte").first()?.text()?.let { parseChapterDate(it) } ?: 0
    }

    private fun parseChapterDate(date: String): Long {
        val dateWords : List<String> = date.split(" ")

        if (dateWords.size == 3) {
            val timeAgo = Integer.parseInt(dateWords[0])
            var date : Calendar = Calendar.getInstance()

            if (dateWords[1].contains("Minute")) {
                date.add(Calendar.MINUTE, - timeAgo)
            } else if (dateWords[1].contains("Hour")) {
                date.add(Calendar.HOUR_OF_DAY, - timeAgo)
            } else if (dateWords[1].contains("Day")) {
                date.add(Calendar.DAY_OF_YEAR, -timeAgo)
            } else if (dateWords[1].contains("Week")) {
                date.add(Calendar.WEEK_OF_YEAR, -timeAgo)
            } else if (dateWords[1].contains("Month")) {
                date.add(Calendar.MONTH, -timeAgo)
            } else if (dateWords[1].contains("Year")) {
                date.add(Calendar.YEAR, -timeAgo)
            }

            return date.getTimeInMillis()
        }

        return 0L
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        document.select("ul.list-switcher-2 > li > select.jump-menu").first().getElementsByTag("option").forEach {
            pages.add(Page(pages.size, it.attr("value")))
        }
        pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
    }

    override fun imageUrlParse(document: Document) = document.select("img.img-responsive-2").first().attr("src")

}