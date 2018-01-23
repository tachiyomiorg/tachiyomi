package eu.kanade.tachiyomi.source.online.french

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class MangaLel : ParsedHttpSource() {

    override val id: Long = 11

    override val name = "Manga-LEL"

    override val baseUrl = "https://www.manga-lel.com"

    override val lang = "fr"

    override val supportsLatest = true

    private val catalogHeaders = Headers.Builder().apply {
        add("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
        add("Host", "www.manga-lel.com")
    }.build()

    private val trustManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }

        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }
    }

    private val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, arrayOf(trustManager), SecureRandom())
    }

    override val client = super.client.newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()

    /**
     * Selectors
     */
    override fun popularMangaSelector() = ".col-sm-6"

    override fun popularMangaNextPageSelector() = ".pagination a[rel='next']"

    override fun latestUpdatesSelector() = ".mangalist .manga-item"

    override fun latestUpdatesNextPageSelector() = ".pagination a[rel='next']"

    override fun searchMangaSelector() = ".col-sm-6"

    override fun searchMangaNextPageSelector() = ".pagination a[rel='next']"

    override fun chapterListSelector() = ".chapters li"

    /**
     * "EMM" (Element Manga Mapping)
     */
    override fun popularMangaFromElement(element: Element): SManga {
        return searchMangaFromElement(element)
    }

    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select(".chart-title").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select(".manga-heading a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select(".date-chapter-title-rtl").first()?.text()?.let { chapterDateParse(it) } ?: 0
        return chapter
    }

    /**
     * Requests
     */
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/filterList?page=$page&sortBy=views&asc=false")
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = HttpUrl.parse(baseUrl)!!.newBuilder()
        val body = FormBody.Builder()
        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is Status -> url.addQueryParameter("status[]", "2")
                is TextField -> url.addQueryParameter(filter.key, filter.state)
                is KeyValueListField<*> -> filter.state.forEach { kvpair ->
                    if ((kvpair as Filter.TriState).state == Filter.TriState.STATE_INCLUDE) {
                        when (kvpair) {
                            is Genre -> url.addQueryParameter("categories[]", kvpair.id.toString())
                            is Type -> url.addQueryParameter("types[]", kvpair.id.toString())
                        }
                    }
                }
            }
        }

        if (!query.isEmpty()) url.addQueryParameter("keyword", query)
        body.add("params", url.build()?.query().orEmpty())
        return POST("$baseUrl/advSearchFilter", catalogHeaders, body.build())
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/latest-release?page=$page")
    }

    /**
     * Parsers
     */
    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        document.select("#all img")?.forEach {
            pages.add(Page(pages.size, "", it.attr("data-src")))
        }
        return pages
    }

    override fun imageUrlParse(document: Document) = ""

    override fun mangaDetailsParse(document: Document): SManga {
        val rightBlockInfo = document.select(".col-sm-8 > .dl-horizontal").first()
        val listOfInfos = rightBlockInfo.select("dd")

        val manga = SManga.create()
        manga.genre = listOfInfos[7]?.text().orEmpty().replace(" ,  ", ", ")
        manga.author = listOfInfos[4]?.text()
        manga.artist = listOfInfos[5]?.text()
        manga.description = document.select(".well > p").text() + "\nScan team : " + listOfInfos[1].text()
        manga.status = listOfInfos[2]?.text().orEmpty().let { mangaStatusParse(it) }
        manga.thumbnail_url = document.select(".boxed > .img-responsive").first()?.attr("src")
        return manga
    }

    private fun mangaStatusParse(status: String) = when {
        status.contains("En cours") -> SManga.ONGOING
        status.contains("Terminé") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    private fun chapterDateParse(date: String): Long {
        try {
            return SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH).parse(date).time
        } catch (e: ParseException) { }
        return 0
    }
    /**
     * Filters
     */
    private class Status : Filter.TriState("Completed")
    private class TextField(name: String, val key: String) : Filter.Text(name)

    private class Genre(name: String, val id: Int) : Filter.TriState(name)
    private class Type(name: String, val id: Int) : Filter.TriState(name)

    private class KeyValueListField<E>(name: String, list: List<E>) : Filter.Group<E>(name, list)

    override fun getFilterList() = FilterList(
            TextField("Author", "author"),
            TextField("Year", "release"),
            Status(),
            KeyValueListField<Genre>("Genre", getGenreList()),
            KeyValueListField<Type>("Type", getTypeList())
    )

    // $(".selectize-dropdown-content").each(function() { console.log("====");   $(this).children().each(function () { console.log("Genre(\"" + $(this).text() + "\", " +  $(this).attr("data-value") + "),"); }) })
    // On => https://www.manga-lel.com/advanced-search

    private fun getGenreList() = listOf(
            Genre("Action", 1),
            Genre("Aventure", 2),
            Genre("Doujinshi", 4),
            Genre("Drame", 5),
            Genre("Ecchi", 6),
            Genre("Fantastique", 7),
            Genre("Transgenre", 8),
            Genre("Harem", 9),
            Genre("Historique", 10),
            Genre("Horreur", 11),
            Genre("Josei", 12),
            Genre("Arts Martiaux", 13),
            Genre("Mature", 14),
            Genre("Mecha", 15),
            Genre("Mystère", 16),
            Genre("One Shot", 17),
            Genre("Psychologique", 18),
            Genre("Romance", 19),
            Genre("Vie scolaire", 20),
            Genre("Science-fiction", 21),
            Genre("Seinen", 22),
            Genre("Shoujo", 23),
            Genre("Shoujo Ai", 24),
            Genre("Shounen", 25),
            Genre("Shonen Ai", 26),
            Genre("Tranche de vie", 27),
            Genre("Sports", 28),
            Genre("Surnaturel", 29),
            Genre("Tragédie", 30),
            Genre("Yaoi", 31),
            Genre("Yuri", 32),
            Genre("Adulte", 33),
            Genre("Fantaisie", 34)
    )

    private fun getTypeList() = listOf(
            Type("Shônen", 1),
            Type("Shôjo", 2),
            Type("Josei", 4),
            Type("Shôjo-ai", 5),
            Type("Shonen-ai", 6),
            Type("Yaoi", 7),
            Type("Yuri", 8),
            Type("Seinen", 9)
    )
}