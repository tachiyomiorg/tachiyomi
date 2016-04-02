package eu.kanade.tachiyomi.data.source.base

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.get
import eu.kanade.tachiyomi.data.network.post
import eu.kanade.tachiyomi.data.source.getLanguages
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class YamlParsedOnlineSource(context: Context, stream: InputStream) : OnlineSource(context) {

    @Suppress("UNCHECKED_CAST")
    val map = YamlSourceNode((Yaml().load(stream) as Map<String, Any?>).withDefault { null })

    override val name: String
        get() = map.name

    override val baseUrl = map.host.let {
        if (it.endsWith("/")) it.dropLast(1) else it
    }

    override val lang = map.lang.toUpperCase().let { code ->
        getLanguages().find { code == it.code }!!
    }

    override val id = map.id.let {
        if (it is Int) it else (lang.code.hashCode() + 31 * it.hashCode()) and 0x7fffffff
    }

    override fun popularMangaRequest(page: MangasPage): Request {
        if (page.page == 1) {
            page.url = map.popular.url
        }
        return when (map.popular.method?.toLowerCase()) {
            "post" -> post(page.url, headers)
            else -> get(page.url, headers)
        }
    }

    override fun parseHtmlToPopularManga(html: String, page: MangasPage) {
        val document = Jsoup.parse(html)
        for (element in document.select(map.popular.manga_css)) {
            Manga().apply {
                source = this@YamlParsedOnlineSource.id
                title = element.text()
                setUrl(element.attr("href"))
                page.mangas.add(this)
            }
        }

        map.popular.next_url_css?.let { query ->
            page.nextPageUrl = document.select(query).first()?.attr("href")?.let {
                when {
                    it.startsWith("http") -> it
                    it.startsWith("/") -> "$baseUrl$it"
                    else -> if (page.url.endsWith("/")) "${page.url}$it" else "${page.url}/$it"
                }
            }
        }
    }

    // Not needed
    override fun getInitialPopularMangaUrl() = ""

    override fun getInitialSearchUrl(query: String) = ""

    override fun searchMangaRequest(page: MangasPage, query: String): Request {
        if (page.page == 1) {
            page.url = map.search.url.replace("\$query", query)
        }
        return when (map.popular.method?.toLowerCase()) {
            "post" -> post(page.url, headers)
            else -> get(page.url, headers)
        }
    }

    override fun parseSearchFromHtml(html: String, page: MangasPage, query: String) {
        val document = Jsoup.parse(html)
        for (element in document.select(map.search.manga_css)) {
            Manga().apply {
                source = this@YamlParsedOnlineSource.id
                title = element.text()
                setUrl(element.attr("href"))
                page.mangas.add(this)
            }
        }

        map.search.next_url_css?.let { query ->
            page.nextPageUrl = document.select(query).first()?.attr("href")?.let {
                when {
                    it.startsWith("http") -> it
                    it.startsWith("/") -> "$baseUrl$it"
                    else -> if (page.url.endsWith("/")) "${page.url}$it" else "${page.url}/$it"
                }
            }
        }
    }

    override fun parseHtmlToManga(html: String, manga: Manga) {
        val document = Jsoup.parse(html)
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

    override fun parseHtmlToChapters(html: String, chapters: MutableList<Chapter>) {
        val document = Jsoup.parse(html)
        with(map.chapters) {
            val pool = emptyMap<String, Element>()
            val dateFormat = SimpleDateFormat(date?.format, Locale.ENGLISH)

            for (element in document.select(chapter_css)) {
                val chapter = Chapter.create()
                element.select(title).first().let {
                    chapter.name = it.text()
                    chapter.setUrl(it.attr("href"))
                }
                val dateElement = element.select(date?.select).first()
                chapter.date_upload = date?.getDate(dateElement, pool, dateFormat)?.time ?: 0
                chapters.add(chapter)
            }
        }
    }

    override fun parseHtmlToPages(response: Response, html: String, pages: MutableList<Page>) {
        val document = Jsoup.parse(html)
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
                val page = pages.getOrElse(i) { Page(i, "").apply { pages.add(this) } }
                page.imageUrl = element.attr(image_attr).let {
                    when {
                        it.startsWith("http") -> it
                        it.startsWith("/") -> "$baseUrl$it"
                        else -> throw Exception("Unsupported pattern")
                    }
                }
            }
        }

    }

    override fun parseHtmlToImageUrl(html: String): String {
        val document = Jsoup.parse(html)
        return with(map.pages) {
            document.select(image_css).first().attr(image_attr).let {
                when {
                    it.startsWith("http") -> it
                    it.startsWith("/") -> "$baseUrl$it"
                    else -> throw Exception("Unsupported pattern")
                }
            }
        }
    }

}
