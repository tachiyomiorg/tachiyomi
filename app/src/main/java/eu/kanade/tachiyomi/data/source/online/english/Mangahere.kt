package eu.kanade.tachiyomi.data.source.online.english

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import okhttp3.HttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class Mangahere(override val id: Int) : ParsedOnlineSource() {

    override val name = "Mangahere"

    override val baseUrl = "http://www.mangahere.co"

    override val lang = "en"

    override val supportsLatest = true

    override fun popularMangaInitialUrl() = "$baseUrl/directory/?views.za"

    override fun latestUpdatesInitialUrl() = "$baseUrl/directory/?last_chapter_time.za"

    override fun popularMangaSelector() = "div.directory_list > ul > li"

    override fun latestUpdatesSelector() = "div.directory_list > ul > li"

    private fun mangaFromElement(query: String, element: Element, manga: Manga) {
        element.select(query).first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = if (it.hasAttr("title")) it.attr("title") else if (it.hasAttr("rel")) it.attr("rel") else it.text()
        }
    }

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        mangaFromElement("div.title > a", element, manga)
    }

    override fun latestUpdatesFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun popularMangaNextPageSelector() = "div.next-page > a.next"

    override fun latestUpdatesNextPageSelector() = "div.next-page > a.next"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter<*>>): String {
        val url = HttpUrl.parse("$baseUrl/search.php?name_method=cw&author_method=cw&artist_method=cw&advopts=1").newBuilder().addQueryParameter("name", query)
        for (filter in if (filters.isEmpty()) this@Mangahere.filters else filters) {
            when (filter) {
                is Status -> url.addQueryParameter("is_completed", arrayOf("", "1", "0")[filter.state])
                is Genre -> url.addQueryParameter(filter.id, filter.state.toString())
                is TextField -> url.addQueryParameter(filter.key, filter.state)
                is ListField -> url.addQueryParameter(filter.key, filter.values[filter.state].value)
                is Order -> url.addQueryParameter("order", if (filter.state) "az" else "za")
            }
        }
        return url.toString()
    }


    override fun searchMangaSelector() = "div.result_search > dl:has(dt)"

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        mangaFromElement("a.manga_info", element, manga)
    }

    override fun searchMangaNextPageSelector() = "div.next-page > a.next"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val detailElement = document.select(".manga_detail_top").first()
        val infoElement = detailElement.select(".detail_topText").first()

        manga.author = infoElement.select("a[href^=http://www.mangahere.co/author/]").first()?.text()
        manga.artist = infoElement.select("a[href^=http://www.mangahere.co/artist/]").first()?.text()
        manga.genre = infoElement.select("li:eq(3)").first()?.text()?.substringAfter("Genre(s):")
        manga.description = infoElement.select("#show").first()?.text()?.substringBeforeLast("Show less")
        manga.status = infoElement.select("li:eq(6)").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = detailElement.select("img.img").first()?.attr("src")
    }

    private fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = ".detail_list > ul:not([class]) > li"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val parentEl = element.select("span.left").first()

        val urlElement = parentEl.select("a").first()

        var volume = parentEl.select("span.mr6")?.first()?.text()?.trim() ?: ""
        if (volume.length > 0) {
            volume = " - " + volume
        }

        var title = parentEl?.textNodes()?.last()?.text()?.trim() ?: ""
        if (title.length > 0) {
            title = " - " + title
        }

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text() + volume + title
        chapter.date_upload = element.select("span.right").first()?.text()?.let { parseChapterDate(it) } ?: 0
    }

    private fun parseChapterDate(date: String): Long {
        return if ("Today" in date) {
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

    override fun pageListParse(document: Document, pages: MutableList<Page>) {
        document.select("select.wid60").first()?.getElementsByTag("option")?.forEach {
            pages.add(Page(pages.size, it.attr("value")))
        }
        pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
    }

    override fun imageUrlParse(document: Document) = document.getElementById("image").attr("src")

    private data class ListValue(val name: String, val value: String) {
        override fun toString(): String = name
    }

    private class Status() : Filter.TriState("Completed")
    private class Genre(name: String, val id: String = "genres[$name]") : Filter.TriState(name)
    private class TextField(name: String, val key: String) : Filter.Text(name)
    private class ListField(name: String, val key: String, values: Array<ListValue>, state: Int = 0) : Filter.List<ListValue>(name, values, state)
    private class Order() : Filter.CheckBox("Ascending order")

    // [...document.querySelectorAll("select[id^='genres'")].map((el,i) => `Genre("${el.nextSibling.nextSibling.textContent.trim()}", "${el.getAttribute('name')}")`).join(',\n')
    // http://www.mangahere.co/advsearch.htm
    override fun getFilterList(): List<Filter<*>> = listOf(
            TextField("Author", "author"),
            TextField("Artist", "artist"),
            ListField("Type", "direction", arrayOf(ListValue("Any", ""), ListValue("Japanese Manga (read from right to left)", "rl"), ListValue("Korean Manhwa (read from left to right)", "lr"))),
            Status(),
            ListField("Order by", "sort", arrayOf(ListValue("Series name", "name"), ListValue("Rating", "rating"), ListValue("Views", "views"), ListValue("Total chapters", "total_chapters"), ListValue("Last chapter", "last_chapter_time")), 2),
            Order(),
            Filter.Header("Genres"),
            Genre("Action"),
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
            Genre("Sports"),
            Genre("Supernatural"),
            Genre("Tragedy"),
            Genre("Yaoi"),
            Genre("Yuri")
    )

}