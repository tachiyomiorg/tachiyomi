package eu.kanade.tachiyomi.data.source.online

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.POST
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.attrOrText
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

class YamlOnlineSource(mappings: Map<*, *>) : OnlineSource() {

    val map = YamlSourceNode(mappings)

    override val name: String
        get() = map.name

    override val baseUrl = map.host.let {
        if (it.endsWith("/")) it.dropLast(1) else it
    }

    override val lang = map.lang.toLowerCase()

    override val supportsLatest = map.latestupdates != null

    override val client = when(map.client) {
        "cloudflare" -> network.cloudflareClient
        else -> network.client
    }

    override val id = map.id.let {
        if (it is Int) it else (lang.toUpperCase().hashCode() + 31 * it.hashCode()) and 0x7fffffff
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

    override fun searchMangaRequest(page: MangasPage, query: String, filters: List<Filter>): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query, filters)
        }
        return when (map.search.method?.toLowerCase()) {
            "post" -> POST(page.url, headers, map.search.createForm())
            else -> GET(page.url, headers)
        }
    }

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) = map.search.url.replace("\$query", query)

    override fun searchMangaParse(response: Response, page: MangasPage, query: String, filters: List<Filter>) {
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

    override fun latestUpdatesRequest(page: MangasPage): Request {
        if (page.page == 1) {
            page.url = latestUpdatesInitialUrl()
        }
        return when (map.latestupdates!!.method?.toLowerCase()) {
            "post" -> POST(page.url, headers, map.latestupdates.createForm())
            else -> GET(page.url, headers)
        }
    }

    override fun latestUpdatesInitialUrl() = map.latestupdates!!.url

    override fun latestUpdatesParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(map.latestupdates!!.manga_css)) {
            Manga.create(id).apply {
                title = element.text()
                setUrlWithoutDomain(element.attr("href"))
                page.mangas.add(this)
            }
        }

        map.latestupdates.next_url_css?.let { selector ->
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
        val body = response.body().string()
        val url = response.request().url().toString()

        // TODO lazy initialization in Kotlin 1.1
        val document = Jsoup.parse(body, url)

        with(map.pages) {
            // Capture a list of values where page urls will be resolved.
            val capturedPages = if (pages_regex != null)
                pages_regex!!.toRegex().findAll(body).map { it.value }.toList()
            else if (pages_css != null)
                document.select(pages_css).map { it.attrOrText(pages_attr!!) }
            else
                null

            // For each captured value, obtain the url and create a new page.
            capturedPages?.forEach { value ->
                // If the captured value isn't an url, we have to use replaces with the chapter url.
                val pageUrl = if (replace != null && replacement != null)
                    url.replace(replace!!.toRegex(), replacement!!.replace("\$value", value))
                else
                    value

                pages.add(Page(pages.size, pageUrl))
            }

            // Capture a list of images.
            val capturedImages = if (image_regex != null)
                image_regex!!.toRegex().findAll(body).map { it.groups[1]?.value }.toList()
            else if (image_css != null)
                document.select(image_css).map { it.absUrl(image_attr) }
            else
                null

            // Assign the image url to each page
            capturedImages?.forEachIndexed { i, url ->
                val page = pages.getOrElse(i) { Page(i, "").apply { pages.add(this) } }
                page.imageUrl = url
            }
        }
    }

    override fun imageUrlParse(response: Response): String {
        val body = response.body().string()
        val url = response.request().url().toString()

        with(map.pages) {
            return if (image_regex != null)
                image_regex!!.toRegex().find(body)!!.groups[1]!!.value
            else if (image_css != null)
                Jsoup.parse(body, url).select(image_css).first().absUrl(image_attr)
            else
                throw Exception("image_regex and image_css are null")
        }
    }
}
