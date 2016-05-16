package eu.kanade.tachiyomi.data.source.base

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * A simple implementation for sources from a website using Jsoup, an HTML parser.
 *
 * @param context the application context.
 */
abstract class ParsedOnlineSource(context: Context) : OnlineSource(context) {

    /**
     * Parse the response from the site and fills [page].
     *
     * @param response the response from the site.
     * @param page the page object to be filled.
     */
    override fun popularMangaParse(response: Response, page: MangasPage) {
        val document = Jsoup.parse(response.body().string())
        for (element in document.select(popularMangaSelector())) {
            Manga().apply {
                source = this@ParsedOnlineSource.id
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(popularMangaNextPageSelector()).first()?.attr("href")?.let {
            when {
                it.startsWith("http") -> it
                it.startsWith("/") -> "$baseUrl$it"
                else -> if (it.endsWith("/")) "${page.url}$it" else "${page.url}/$it"
            }
        }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    abstract protected fun popularMangaSelector(): String

    /**
     * Fills [manga] with the given [element]. Most sites only show the title and the url, it's
     * totally safe to fill only those two values.
     *
     * @param element an element obtained from [popularMangaSelector].
     * @param manga the manga to fill.
     */
    abstract protected fun popularMangaFromElement(element: Element, manga: Manga)

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    abstract protected fun popularMangaNextPageSelector(): String?

    /**
     * Parse the response from the site and fills [page].
     *
     * @param response the response from the site.
     * @param page the page object to be filled.
     * @param query the search query.
     */
    override fun searchMangaParse(response: Response, page: MangasPage, query: String) {
        val document = Jsoup.parse(response.body().string())
        for (element in document.select(searchMangaSelector())) {
            Manga().apply {
                source = this@ParsedOnlineSource.id
                searchMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(searchMangaNextPageSelector()).first()?.attr("href")?.let {
            when {
                it.startsWith("http") -> it
                it.startsWith("/") -> "$baseUrl$it"
                else -> if (it.endsWith("/")) "${page.url}$it" else "${page.url}/$it"
            }
        }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    abstract protected fun searchMangaSelector(): String

    /**
     * Fills [manga] with the given [element]. Most sites only show the title and the url, it's
     * totally safe to fill only those two values.
     *
     * @param element an element obtained from [searchMangaSelector].
     * @param manga the manga to fill.
     */
    abstract protected fun searchMangaFromElement(element: Element, manga: Manga)

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    abstract protected fun searchMangaNextPageSelector(): String?

    /**
     * Parse the response from the site and fills the details of [manga].
     *
     * @param response the response from the site.
     * @param manga the manga to fill.
     */
    override fun mangaDetailsParse(response: Response, manga: Manga) {
        mangaDetailsParse(Jsoup.parse(response.body().string()), manga)
    }

    /**
     * Fills the details of [manga] from the given [document].
     *
     * @param document the parsed document.
     * @param manga the manga to fill.
     */
    abstract protected fun mangaDetailsParse(document: Document, manga: Manga)

    /**
     * Parse the response from the site and fills the chapter list.
     *
     * @param response the response from the site.
     * @param chapters the list of chapters to fill.
     */
    override fun chapterListParse(response: Response, chapters: MutableList<Chapter>) {
        val document = Jsoup.parse(response.body().string())

        for (element in document.select(chapterListSelector())) {
            Chapter.create().apply {
                chapterFromElement(element, this)
                chapters.add(this)
            }
        }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each chapter.
     */
    abstract protected fun chapterListSelector(): String

    /**
     * Fills [chapter] with the given [element].
     *
     * @param element an element obtained from [chapterListSelector].
     * @param chapter the chapter to fill.
     */
    abstract protected fun chapterFromElement(element: Element, chapter: Chapter)

    /**
     * Parse the response from the site and fills the page list.
     *
     * @param response the response from the site.
     * @param pages the list of pages to fill.
     */
    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        pageListParse(Jsoup.parse(response.body().string()), pages)
    }

    /**
     * Fills [pages] from the given [document].
     *
     * @param document the parsed document.
     * @param pages the list of pages to fill.
     */
    abstract protected fun pageListParse(document: Document, pages: MutableList<Page>)

    /**
     * Parse the response from the site and returns the absolute url to the source image.
     *
     * @param response the response from the site.
     */
    override fun imageUrlParse(response: Response): String {
        return imageUrlParse(Jsoup.parse(response.body().string()))
    }

    /**
     * Returns the absolute url to the source image from the document.
     *
     * @param document the parsed document.
     */
    abstract protected fun imageUrlParse(document: Document): String

}
