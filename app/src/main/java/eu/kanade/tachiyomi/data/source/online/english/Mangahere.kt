package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class Mangahere(context: Context, override val id: Int) : ParsedOnlineSource(context) {

    override val name = "Mangahere"

    override val baseUrl = "http://www.mangahere.co"

    override val lang: Language get() = EN

    override fun popularMangaInitialUrl() = "$baseUrl/directory/"

    override fun popularMangaSelector() = "div.directory_list > ul > li"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("div.title > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun popularMangaNextPageSelector() = "div.next-page > a.next"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/search.php?name=$query&page=1&sort=views&order=za"

    override fun searchMangaSelector() = "div.result_search > dl:has(dt)"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.select("a.manga_info").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun searchMangaNextPageSelector() = "div.next-page > a.next"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val detailElement = document.select(".manga_detail_top").first()
        val infoElement = detailElement.select(".detail_topText").first()

        manga.author = infoElement.select("a[href^=http://www.mangahere.co/author/]").first()?.text()
        manga.artist = infoElement.select("a[href^=http://www.mangahere.co/artist/]").first()?.text()
        manga.genre = infoElement.select("li:eq(3)").first()?.text()?.substringAfter("Genre(s):")
        manga.description = infoElement.select("#show").first()?.text()?.substringBeforeLast("Show less")
        manga.status = infoElement.select("li:eq(6)").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = detailElement.select("img.img").first()?.attr("src")
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = ".detail_list > ul:not([class]) > li"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("span.right").first()?.text()?.let { parseChapterDate(it) } ?: 0
    }

    private fun parseChapterDate(date: String): Long {
        return if ("Today" in date) {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else if ("Yesterday" in date) {
            Calendar.getInstance().apply {
                add(Calendar.DATE, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            try {
                SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).parse(date).time
            } catch (e: ParseException) {
                0L
            }
        }
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        document.select("select.wid60").first()?.getElementsByTag("option")?.forEach {
            pages.add(Page(pages.size, it.attr("value")))
        }
        pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
    }

    override fun imageUrlParse(document: Document) = document.getElementById("image").attr("src")

}