package eu.kanade.tachiyomi.data.source.online.german

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.DE
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat

class WieManga(context: Context, override val id: Int) : ParsedOnlineSource(context) {

        override val name = "Wie Manga!"

        override val baseUrl = "http://www.wiemanga.com"

        override val lang: Language get() = DE

        override fun popularMangaInitialUrl() = "$baseUrl/list/Hot-Book/"

        override fun popularMangaSelector() = ".booklist td > div"

        override fun popularMangaFromElement(element: Element, manga: Manga) {
                val title = element.select("dd a:first-child")

                manga.setUrlWithoutDomain(title.attr("href"))
                manga.title = title.text()
                manga.thumbnail_url = element.select("dt img").attr("src")
        }

        override fun popularMangaNextPageSelector() = null

        override fun searchMangaInitialUrl(query: String, filters: List<Filter>) = "$baseUrl/search/?wd=$query"

        override fun searchMangaSelector() = ".searchresult td > div"

        override fun searchMangaFromElement(element: Element, manga: Manga) {
                val title = element.select(".resultbookname")

                manga.setUrlWithoutDomain(title.attr("href"))
                manga.title = title.text()
                manga.thumbnail_url = element.select(".resultimg img").attr("src")
        }

        override fun searchMangaNextPageSelector() = ".pagetor a.l"

        override fun mangaDetailsParse(document: Document, manga: Manga) {
                val infoElement = document.select(".bookmessgae tr > td:nth-child(2)").first()

                manga.author = infoElement.select("dd:nth-of-type(2) a").first()?.text()
                manga.artist = infoElement.select("dd:nth-of-type(3) a").first()?.text()
                manga.description = infoElement.select("dl > dt:last-child").first()?.text()?.replaceFirst("Beschreibung", "")
                manga.thumbnail_url = document.select(".bookmessgae tr > td:nth-child(1) a > img").first()?.attr("src")

                if (manga.author == "RSS")
                        manga.author = null

                if (manga.artist == "RSS")
                        manga.artist = null
        }

        override fun chapterListSelector() = ".chapterlist tr:not(:first-child)"

        override fun chapterFromElement(element: Element, chapter: Chapter) {
                val urlElement = element.select(".col1 a").first()
                val dateElement = element.select(".col3 a").first()

                chapter.setUrlWithoutDomain(urlElement.attr("href"))
                chapter.name = urlElement.text()
                chapter.date_upload = dateElement?.text()?.let { parseChapterDate(it) } ?: 0
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

        override fun pageListParse(document: Document, pages: MutableList<Page>) {}

        override fun imageUrlParse(document: Document) = document.select("img#comicpic").first().attr("src")

}