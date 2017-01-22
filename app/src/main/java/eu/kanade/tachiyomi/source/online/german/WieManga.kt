package eu.kanade.tachiyomi.source.online.german

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat

class WieManga : ParsedHttpSource() {

    override val id: Long = 10

    override val name = "Wie Manga!"

    override val baseUrl = "http://www.wiemanga.com"

    override val lang = "de"

    override val supportsLatest = true

    override fun popularMangaSelector() = ".booklist td > div"

    override fun latestUpdatesSelector() = ".booklist td > div"

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/list/Hot-Book/", headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/list/New-Update/", headers)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val image = element.select("dt img")
        val title = element.select("dd a:first-child")

        val manga = SManga.create()
        manga.setUrlWithoutDomain(title.attr("href"))
        manga.title = title.text()
        manga.thumbnail_url = image.attr("src")
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun popularMangaNextPageSelector() = null

    override fun latestUpdatesNextPageSelector() = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search/?wd=$query", headers)
    }

    override fun searchMangaSelector() = ".searchresult td > div"

    override fun searchMangaFromElement(element: Element): SManga {
        val image = element.select(".resultimg img")
        val title = element.select(".resultbookname")

        val manga = SManga.create()
        manga.setUrlWithoutDomain(title.attr("href"))
        manga.title = title.text()
        manga.thumbnail_url = image.attr("src")
        return manga
    }

    override fun searchMangaNextPageSelector() = ".pagetor a.l"

    override fun mangaDetailsParse(document: Document): SManga {
        val imageElement = document.select(".bookmessgae tr > td:nth-child(1)").first()
        val infoElement = document.select(".bookmessgae tr > td:nth-child(2)").first()

        val manga = SManga.create()
        manga.author = infoElement.select("dd:nth-of-type(2) a").first()?.text()
        manga.artist = infoElement.select("dd:nth-of-type(3) a").first()?.text()
        manga.description = infoElement.select("dl > dt:last-child").first()?.text()?.replaceFirst("Beschreibung", "")
        manga.thumbnail_url = imageElement.select("img").first()?.attr("src")

        if (manga.author == "RSS")
            manga.author = null

        if (manga.artist == "RSS")
            manga.artist = null
        return manga
    }

    override fun chapterListSelector() = ".chapterlist tr:not(:first-child)"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select(".col1 a").first()
        val dateElement = element.select(".col3 a").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = dateElement?.text()?.let { parseChapterDate(it) } ?: 0
        return chapter
    }

    private fun parseChapterDate(date: String): Long {
        return SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(date).time
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()

        document.select("select#page").first().select("option").forEach {
            pages.add(Page(pages.size, it.attr("value")))
        }
        return pages
    }

    override fun imageUrlParse(document: Document) = document.select("img#comicpic").first().attr("src")

}