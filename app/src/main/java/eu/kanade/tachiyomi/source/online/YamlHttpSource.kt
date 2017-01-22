package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.attrOrText
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

class YamlHttpSource(mappings: Map<*, *>) : HttpSource() {

    val map = YamlSourceNode(mappings)

    override val name: String
        get() = map.name

    override val baseUrl = map.host.let {
        if (it.endsWith("/")) it.dropLast(1) else it
    }

    override val lang = map.lang.toLowerCase()

    override val supportsLatest = map.latestupdates != null

    override val client = when (map.client) {
        "cloudflare" -> network.cloudflareClient
        else -> network.client
    }

    override val id = map.id.let {
        (it as? Int ?: (lang.toUpperCase().hashCode() + 31 * it.hashCode()) and 0x7fffffff).toLong()
    }

    // Ugly, but needed after the changes
    var popularNextPage: String? = null
    var searchNextPage: String? = null
    var latestNextPage: String? = null

    override fun popularMangaRequest(page: Int): Request {
        val url = if (page == 1) {
            popularNextPage = null
            map.popular.url
        } else {
            popularNextPage!!
        }
        return when (map.popular.method?.toLowerCase()) {
            "post" -> POST(url, headers, map.popular.createForm())
            else -> GET(url, headers)
        }
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(map.popular.manga_css).map { element ->
            SManga.create().apply {
                title = element.text()
                setUrlWithoutDomain(element.attr("href"))
            }
        }

        popularNextPage = map.popular.next_url_css?.let { selector ->
             document.select(selector).first()?.absUrl("href")
        }

        return MangasPage(mangas, popularNextPage != null)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = if (page == 1) {
            searchNextPage = null
            map.search.url.replace("\$query", query)
        } else {
            searchNextPage!!
        }
        return when (map.search.method?.toLowerCase()) {
            "post" -> POST(url, headers, map.search.createForm())
            else -> GET(url, headers)
        }
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(map.search.manga_css).map { element ->
            SManga.create().apply {
                title = element.text()
                setUrlWithoutDomain(element.attr("href"))
            }
        }

        searchNextPage = map.search.next_url_css?.let { selector ->
            document.select(selector).first()?.absUrl("href")
        }

        return MangasPage(mangas, searchNextPage != null)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val url = if (page == 1) {
            latestNextPage = null
            map.latestupdates!!.url
        } else {
            latestNextPage!!
        }
        return when (map.latestupdates!!.method?.toLowerCase()) {
            "post" -> POST(url, headers, map.latestupdates.createForm())
            else -> GET(url, headers)
        }
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(map.latestupdates!!.manga_css).map { element ->
            SManga.create().apply {
                title = element.text()
                setUrlWithoutDomain(element.attr("href"))
            }
        }

        popularNextPage = map.latestupdates.next_url_css?.let { selector ->
            document.select(selector).first()?.absUrl("href")
        }

        return MangasPage(mangas, popularNextPage != null)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()

        val manga = SManga.create()
        with(map.manga) {
            val pool = parts.get(document)

            manga.author = author?.process(document, pool)
            manga.artist = artist?.process(document, pool)
            manga.description = summary?.process(document, pool)
            manga.thumbnail_url = cover?.process(document, pool)
            manga.genre = genres?.process(document, pool)
            manga.status = status?.getStatus(document, pool) ?: SManga.UNKNOWN
        }
        return manga
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()

        val chapters = mutableListOf<SChapter>()
        with(map.chapters) {
            val pool = emptyMap<String, Element>()
            val dateFormat = SimpleDateFormat(date?.format, Locale.ENGLISH)

            for (element in document.select(chapter_css)) {
                val chapter = SChapter.create()
                element.select(title).first().let {
                    chapter.name = it.text()
                    chapter.setUrlWithoutDomain(it.attr("href"))
                }
                val dateElement = element.select(date?.select).first()
                chapter.date_upload = date?.getDate(dateElement, pool, dateFormat)?.time ?: 0
                chapters.add(chapter)
            }
        }
        return chapters
    }

    override fun pageListParse(response: Response): List<Page> {
        val body = response.body().string()
        val url = response.request().url().toString()

        val pages = mutableListOf<Page>()

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
        return pages
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
