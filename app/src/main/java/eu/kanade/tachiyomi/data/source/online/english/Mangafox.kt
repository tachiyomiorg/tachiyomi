package eu.kanade.tachiyomi.data.source.online.english

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class Mangafox(override val id: Int) : ParsedOnlineSource() {

    override val name = "Mangafox"

    override val baseUrl = "http://mangafox.me"

    override val lang = "en"

    override val supportsLatest = true

    override fun popularMangaInitialUrl() = "$baseUrl/directory/"

    override fun latestUpdatesInitialUrl() = "$baseUrl/directory/?latest"

    override fun popularMangaSelector() = "div#mangalist > ul.list > li"

    override fun latestUpdatesSelector() = "div#mangalist > ul.list > li"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("a.title").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun latestUpdatesFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun popularMangaNextPageSelector() = "a:has(span.next)"

    override fun latestUpdatesNextPageSelector() = "a:has(span.next)"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter<*>>): String {
        val url = HttpUrl.parse("$baseUrl/search.php?name_method=cw&author_method=cw&artist_method=cw&advopts=1").newBuilder().addQueryParameter("name", query)
        for (filter in if (filters.isEmpty()) this@Mangafox.filters else filters) {
            when (filter) {
                is Genre -> url.addQueryParameter(filter.id, filter.state.toString())
                is TextField -> url.addQueryParameter(filter.key, filter.state)
                is ListField -> url.addQueryParameter(filter.key, filter.values[filter.state].value)
                is Order -> url.addQueryParameter("order", if (filter.state) "az" else "za")
            }
        }
        return url.toString()
    }

    override fun searchMangaSelector() = "div#mangalist > ul.list > li"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        element.select("a.title").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
    }

    override fun searchMangaNextPageSelector() = "a:has(span.next)"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val infoElement = document.select("div#title").first()
        val rowElement = infoElement.select("table > tbody > tr:eq(1)").first()
        val sideInfoElement = document.select("#series_info").first()

        manga.author = rowElement.select("td:eq(1)").first()?.text()
        manga.artist = rowElement.select("td:eq(2)").first()?.text()
        manga.genre = rowElement.select("td:eq(3)").first()?.text()
        manga.description = infoElement.select("p.summary").first()?.text()
        manga.status = sideInfoElement.select(".data").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = sideInfoElement.select("div.cover > img").first()?.attr("src")
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "div#chapters li div"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a.tips").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("span.date").first()?.text()?.let { parseChapterDate(it) } ?: 0
    }

    private fun parseChapterDate(date: String): Long {
        return if ("Today" in date || " ago" in date) {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else if ("Yesterday" in date) {
            Calendar.getInstance().apply {
                add(Calendar.DATE, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            try {
                SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).parse(date).time
            } catch (e: ParseException) {
                0L
            }
        }
    }

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        val document = response.asJsoup()

        val url = response.request().url().toString().substringBeforeLast('/')
        document.select("select.m").first()?.select("option:not([value=0])")?.forEach {
            pages.add(Page(pages.size, "$url/${it.attr("value")}.html"))
        }
    }

    // Not used, overrides parent.
    override fun pageListParse(document: Document, pages: MutableList<Page>) {
    }

    override fun imageUrlParse(document: Document) = document.getElementById("image").attr("src")

    private data class ListValue(val name: String, val value: String) {
        override fun toString(): String = name
    }

    private class Genre(name: String, val id: String = "genres[$name]") : Filter.TriState(name)
    private class TextField(name: String, val key: String) : Filter.Text(name)
    private class ListField(name: String, val key: String, values: Array<ListValue>, state: Int = 0) : Filter.List<ListValue>(name, values, state)
    private class Order() : Filter.CheckBox("Ascending order")

    // $('select.genres').map((i,el)=>`Genre("${$(el).next().text().trim()}", "${$(el).attr('name')}")`).get().join(',\n')
    // on http://mangafox.me/search.php
    override fun getFilterList(): List<Filter<*>> = listOf(
            TextField("Author", "author"),
            TextField("Artist", "artist"),
            ListField("Type", "type", arrayOf(ListValue("Any", ""), ListValue("Japanese Manga", "1"), ListValue("Korean Manhwa", "2"), ListValue("Chinese Manhua", "3"))),
            Genre("Completed", "is_completed"),
            ListField("Order by", "sort", arrayOf(ListValue("Series name", "name"), ListValue("Rating", "rating"), ListValue("Views", "views"), ListValue("Total chapters", "total_chapters"), ListValue("Last chapter", "last_chapter_time")), 2),
            Order(),
            Filter.Header("Genres"),
            Genre("Action"),
            Genre("Adult"),
            Genre("Adventure"),
            Genre("Comedy"),
            Genre("Doujinshi"),
            Genre("Drama"),
            Genre("Ecchi"),
            Genre("Fantasy"),
            Genre("Gender Bender"),
            Genre("Harem"),
            Genre("Historical"),
            Genre("Horror"),
            Genre("Josei"),
            Genre("Martial Arts"),
            Genre("Mature"),
            Genre("Mecha"),
            Genre("Mystery"),
            Genre("One Shot"),
            Genre("Psychological"),
            Genre("Romance"),
            Genre("School Life"),
            Genre("Sci-fi"),
            Genre("Seinen"),
            Genre("Shoujo"),
            Genre("Shoujo Ai"),
            Genre("Shounen"),
            Genre("Shounen Ai"),
            Genre("Slice of Life"),
            Genre("Smut"),
            Genre("Sports"),
            Genre("Supernatural"),
            Genre("Tragedy"),
            Genre("Webtoons"),
            Genre("Yaoi"),
            Genre("Yuri")
    )

}