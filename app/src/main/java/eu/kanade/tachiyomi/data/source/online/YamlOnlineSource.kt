package eu.kanade.tachiyomi.data.source.online

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.POST
import eu.kanade.tachiyomi.data.source.getLanguages
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

class YamlOnlineSource(context: Context, mappings: Map<*, *>) : OnlineSource(context) {

    val map = YamlSourceNode(mappings)

    override val name: String
        get() = map.name

    override val baseUrl = map.host.let {
        if (it.endsWith("/")) it.dropLast(1) else it
    }

    override val lang = map.lang.toUpperCase().let { code ->
        getLanguages().find { code == it.code }!!
    }

    override val client = when(map.client) {
        "cloudflare" -> network.cloudflareClient
        else -> network.client
    }

    override val id = map.id.let {
        if (it is Int) it else (lang.code.hashCode() + 31 * it.hashCode()) and 0x7fffffff
    }

    override fun popularMangaRequest(page: MangasPage): Request {
        if (page.page == 1) {
            page.url = popularMangaInitialUrl()
        }
        return when (map.popular.method?.toLowerCase()) {
            "post" -> POST(page.url, headers, map.popular.createForm())
            else -> GET(page.url, headers)
        }
    }

    override fun popularMangaInitialUrl() = map.popular.url

    override fun popularMangaParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(map.popular.manga_css)) {
            Manga.create(id).apply {
                title = element.text()
                setUrlWithoutDomain(element.attr("href"))
                page.mangas.add(this)
            }
        }

        map.popular.next_url_css?.let { selector ->
            page.nextPageUrl = document.select(selector).first()?.absUrl("href")
        }
    }

    override fun searchMangaRequest(page: MangasPage, query: String): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query)
        }
        return when (map.search.method?.toLowerCase()) {
            "post" -> POST(page.url, headers, map.search.createForm())
            else -> GET(page.url, headers)
        }
    }

    override fun searchMangaInitialUrl(query: String) = map.search.url.replace("\$query", query)

    override fun searchMangaParse(response: Response, page: MangasPage, query: String) {
        val document = response.asJsoup()
        for (element in document.select(map.search.manga_css)) {
            Manga.create(id).apply {
                title = element.text()
                setUrlWithoutDomain(element.attr("href"))
                page.mangas.add(this)
            }
        }

        map.search.next_url_css?.let { selector ->
            page.nextPageUrl = document.select(selector).first()?.absUrl("href")
        }
    }

    override fun mangaDetailsParse(response: Response, manga: Manga) {
        val document = response.asJsoup()
        with(map.manga) {
            val pool = parts.get(document)

            manga.author = author?.process(document, pool)
            manga.artist = artist?.process(document, pool)
            manga.description = summary?.process(document, pool)
            manga.thumbnail_url = cover?.process(document, pool)
            manga.genre = genres?.process(document, pool)
            manga.status = status?.getStatus(document, pool) ?: Manga.UNKNOWN
        }
    }

    override fun chapterListParse(response: Response, chapters: MutableList<Chapter>) {
        val document = response.asJsoup()
        with(map.chapters) {
            val pool = emptyMap<String, Element>()
            val dateFormat = SimpleDateFormat(date?.format, Locale.ENGLISH)

            for (element in document.select(chapter_css)) {
                val chapter = Chapter.create()
                element.select(title).first().let {
                    chapter.name = it.text()
                    chapter.setUrlWithoutDomain(it.attr("href"))
                }
                val dateElement = element.select(date?.select).first()
                chapter.date_upload = date?.getDate(dateElement, pool, dateFormat)?.time ?: 0
                chapters.add(chapter)
            }
        }
    }

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        val document = response.asJsoup()
        with(map.pages) {
            val url = response.request().url().toString()
            pages_css?.let {
                for (element in document.select(it)) {
                    val value = element.attr(pages_attr)
                    val pageUrl = replace?.let { url.replace(it.toRegex(), replacement!!.replace("\$value", value)) } ?: value
                    pages.add(Page(pages.size, pageUrl))
                }
            }

            for ((i, element) in document.select(image_css).withIndex()) {
                pages.getOrNull(i)?.imageUrl = element.absUrl(image_attr)
            }
        }

    }

    override fun imageUrlParse(response: Response): String {
        val document = response.asJsoup()
        return with(map.pages) {
            document.select(image_css).first().absUrl(image_attr)
        }
    }

}
