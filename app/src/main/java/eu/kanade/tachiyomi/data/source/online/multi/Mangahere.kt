package eu.kanade.tachiyomi.data.source.online.multi

import android.content.Context
import android.util.Log
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.ES
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.MultiSource
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class Mangahere(context: Context, override val id: Int) : MultiSource(context) {

    override val name = "Mangahere"

    override val lang: Language = lang(listOf(EN, ES))

    override fun baseURL(language: Language, url: String) : String {
        if (lang == EN) {
            return "http://www.$url"
        }
        return "http://${lang.code}.$url"
    }

    override val baseUrl = baseURL(lang, "mangahere.co")

    override fun popularMangaInitialUrl() = "$baseUrl/directory/"

    override fun popularMangaSelector() = "div.directory_list > ul > li"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        //Log.w("Mangahere.popular", element.html())
        element.select("div.title > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            //Log.w("Mangahere.popular.url", manga.url)
            manga.title = it.text()
            //manga.thumbnail_url = it.select("img").attr("src")
        }
    }

    override fun popularMangaNextPageSelector() = "div.next-page > a.next"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) = "$baseUrl/search.php?name=$query&page=1&sort=views&order=za&${filters.map { it.id + "=1" }.joinToString("&")}&advopts=1"

    override fun searchMangaSelector() = "div.result_search > dl:has(dt)"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.select("a.manga_info").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun searchMangaNextPageSelector() = "div.next-page > a.next"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        Log.w("Mangahere.details.doc", document.location())
        val detailElement = document.select(".manga_detail_top").first()
        Log.w("Mangahere.details.el", detailElement.html())
        val infoElement = detailElement.select(".detail_topText").first()
        //Log.w("Mangahere.details.info", infoElement.html())

        manga.thumbnail_url = detailElement.select("img").first()?.attr("src")
        manga.author = infoElement.select("li:eq(4) > a").text()
        if (lang == EN) manga.artist = infoElement.select("li:eq(5) > a").text()
        manga.genre = infoElement.select("li:eq(3) > a").text().substringAfter(':')
        manga.description = infoElement.select("#show").first()?.text()?.substringBeforeLast("Show less")
        if (lang == ES) manga.status = infoElement.select("li:eq(5)").first()?.text().orEmpty().let { parseStatus(it) }
        else manga.status = infoElement.select("li:eq(6)").first()?.text().orEmpty().let { parseStatus(it) }
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("En desarrollo") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        status.contains("Terminado") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "div.detail_list > ul > li"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("span.left > a")

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
                SimpleDateFormat("MMM dd, yyyy").parse(date).time
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

    override fun imageUrlParse(document: Document): String = document.getElementById("image").attr("src")

    // [...document.querySelectorAll("select[id^='genres'")].map((el,i) => `Filter("${el.getAttribute('name')}", "${el.nextSibling.nextSibling.textContent.trim()}")`).join(',\n')
    // http://www.mangahere.co/advsearch.htm
    override fun getFilterList(): List<Filter> = listOf(
            Filter("genres[Action]", "Action"),
            Filter("genres[Adventure]", "Adventure"),
            Filter("genres[Comedy]", "Comedy"),
            Filter("genres[Doujinshi]", "Doujinshi"),
            Filter("genres[Drama]", "Drama"),
            Filter("genres[Ecchi]", "Ecchi"),
            Filter("genres[Fantasy]", "Fantasy"),
            Filter("genres[Gender Bender]", "Gender Bender"),
            Filter("genres[Harem]", "Harem"),
            Filter("genres[Historical]", "Historical"),
            Filter("genres[Horror]", "Horror"),
            Filter("genres[Josei]", "Josei"),
            Filter("genres[Martial Arts]", "Martial Arts"),
            Filter("genres[Mature]", "Mature"),
            Filter("genres[Mecha]", "Mecha"),
            Filter("genres[Mystery]", "Mystery"),
            Filter("genres[One Shot]", "One Shot"),
            Filter("genres[Psychological]", "Psychological"),
            Filter("genres[Romance]", "Romance"),
            Filter("genres[School Life]", "School Life"),
            Filter("genres[Sci-fi]", "Sci-fi"),
            Filter("genres[Seinen]", "Seinen"),
            Filter("genres[Shoujo]", "Shoujo"),
            Filter("genres[Shoujo Ai]", "Shoujo Ai"),
            Filter("genres[Shounen]", "Shounen"),
            Filter("genres[Shounen Ai]", "Shounen Ai"),
            Filter("genres[Slice of Life]", "Slice of Life"),
            Filter("genres[Sports]", "Sports"),
            Filter("genres[Supernatural]", "Supernatural"),
            Filter("genres[Tragedy]", "Tragedy"),
            Filter("genres[Yaoi]", "Yaoi"),
            Filter("genres[Yuri]", "Yuri")
    )
}