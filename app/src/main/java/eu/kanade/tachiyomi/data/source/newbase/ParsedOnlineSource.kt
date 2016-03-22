package eu.kanade.tachiyomi.data.source.newbase

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

abstract class ParsedOnlineSource(context: Context) : OnlineSource(context) {

    override fun parseHtmlToPopularManga(html: String, page: MangasPage) {
        val document = Jsoup.parse(html)
        for (element in document.select(getPopularMangaSelector())) {
            Manga().apply {
                source = this@ParsedOnlineSource.id
                constructPopularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(getNextPopularPageSelector()).first()?.attr("href")?.let {
            when {
                it.startsWith("http") -> it
                it.startsWith("/") -> "$baseUrl$it"
                else -> if (it.endsWith("/")) "${page.url}$it" else "${page.url}/$it"
            }
        }
    }

    abstract fun getPopularMangaSelector(): String

    abstract fun constructPopularMangaFromElement(element: Element, manga: Manga)

    abstract fun getNextPopularPageSelector(): String?

    override fun parseSearchFromHtml(html: String, page: MangasPage) {
        val document = Jsoup.parse(html)
        for (element in document.select(getSearchMangaSelector())) {
            Manga().apply {
                source = this@ParsedOnlineSource.id
                constructSearchMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = document.select(getNextSearchPageSelector()).first()?.attr("href")?.let {
            when {
                it.startsWith("http") -> it
                it.startsWith("/") -> "$baseUrl$it"
                else -> if (it.endsWith("/")) "${page.url}$it" else "${page.url}/$it"
            }
        }
    }

    abstract fun getSearchMangaSelector(): String

    abstract fun constructSearchMangaFromElement(element: Element, manga: Manga)

    abstract fun getNextSearchPageSelector(): String?

    override fun parseHtmlToManga(html: String, manga: Manga) {
        constructMangaFromDocument(Jsoup.parse(html), manga)
    }

    abstract fun constructMangaFromDocument(document: Document, manga: Manga)

    override fun parseHtmlToChapters(html: String, chapters: MutableList<Chapter>) {
        val document = Jsoup.parse(html)

        for (element in document.select(getChapterListSelector())) {
            constructChapterFromElement(element, Chapter.create())
        }
    }

    abstract fun getChapterListSelector(): String

    abstract fun constructChapterFromElement(element: Element, chapter: Chapter)

    override fun parseHtmlToPages(response: Response, html: String, pages: MutableList<Page>) {
        parseDocumentToPages(Jsoup.parse(html), pages)
    }

    abstract fun parseDocumentToPages(document: Document, pages: MutableList<Page>)

    override fun parseHtmlToImageUrl(html: String): String {
        return parseDocumentToImageUrl(Jsoup.parse(html))
    }

    abstract fun parseDocumentToImageUrl(document: Document): String

}
