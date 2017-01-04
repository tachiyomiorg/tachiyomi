package eu.kanade.tachiyomi.data.source.online.german

import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.model.SChapter
import eu.kanade.tachiyomi.data.source.model.SManga
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat

class WieManga(override val id: Int) : ParsedOnlineSource() {

    override val name = "Wie Manga!"

    override val baseUrl = "http://www.wiemanga.com"

    override val lang = "de"

    override val supportsLatest = true

    override fun popularMangaInitialUrl() = "$baseUrl/list/Hot-Book/"

    override fun latestUpdatesInitialUrl() = "$baseUrl/list/New-Update/"

    override fun popularMangaSelector() = ".booklist td > div"

    override fun latestUpdatesSelector() = ".booklist td > div"

    override fun popularMangaFromElement(element: Element, manga: SManga) {
        val image = element.select("dt img")
        val title = element.select("dd a:first-child")

        manga.setUrlWithoutDomain(title.attr("href"))
        manga.title = title.text()
        manga.thumbnail_url = image.attr("src")
    }

    override fun latestUpdatesFromElement(element: Element, manga: SManga) {
        popularMangaFromElement(element, manga)
    }

    override fun popularMangaNextPageSelector() = null

    override fun latestUpdatesNextPageSelector() = null

    override fun searchMangaInitialUrl(query: String, filters: List<Filter<*>>) = "$baseUrl/search/?wd=$query"

    override fun searchMangaSelector() = ".searchresult td > div"

    override fun searchMangaFromElement(element: Element, manga: SManga) {
        val image = element.select(".resultimg img")
        val title = element.select(".resultbookname")

        manga.setUrlWithoutDomain(title.attr("href"))
        manga.title = title.text()
        manga.thumbnail_url = image.attr("src")
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

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        val document = response.asJsoup()

        document.select("select#page").first().select("option").forEach {
            pages.add(Page(pages.size, it.attr("value")))
        }
    }

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
    }

    override fun imageUrlParse(document: Document) = document.select("img#comicpic").first().attr("src")

}