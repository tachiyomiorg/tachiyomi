package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.online.multi.Mangahere
import org.jsoup.nodes.Document

class MangahereEN(context: Context, override val id: Int) : Mangahere(context) {

    override val lang = EN

    override val baseUrl: String = "http://www.mangahere.co"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/search.php?name=$query&page=1&sort=views&order=za&${filters.map { it.id + "=1" }.joinToString("&")}&advopts=1"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val detailElement = document.select(".manga_detail_top").first()
        val infoElement = detailElement.select(".detail_topText").first()

        manga.thumbnail_url = detailElement.childNode(1).attr("src")
        manga.author = infoElement.select("li:eq(4) > a").text()
        manga.artist = infoElement.select("li:eq(5) > a").text()
        manga.genre = infoElement.select("li:eq(3) > a").text().substringAfter(':')
        manga.description = infoElement.select("#show").first()?.text()?.substringBeforeLast("Show less")
        manga.status = infoElement.select("li:eq(6)").first()?.text().orEmpty().let { parseStatus(it) }
    }

    override fun parseStatus(status: String) = when {
        status.contains("Ongoing") -> Manga.ONGOING
        status.contains("Completed") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    // [...document.querySelectorAll("select[id^='genres'")].map((el,i) => `Filter("${el.getAttribute('name')}", "${el.nextSibling.nextSibling.textContent.trim()}")`).join(',\n')
    // http://www.mangahere.co/advsearch.htm
    override fun getFilterList(): List<Filter> = listOf(
            Filter("genres[Action]", "Action"),
            Filter("genres[Adventure]", "Adventure"),
            Filter("genres[Comedy]", "Comedy"),
            Filter("genres[Doujinshi]", "Doujinshi"),
            Filter("genres[Drama]", "Drama"),
            Filter("genres[Ecchi]", "Ecchi"),
            Filter("genres[Fantasy]", "Fantasy"),
            Filter("genres[Gender Bender]", "Gender Bender"),
            Filter("genres[Harem]", "Harem"),
            Filter("genres[Historical]", "Historical"),
            Filter("genres[Horror]", "Horror"),
            Filter("genres[Josei]", "Josei"),
            Filter("genres[Martial Arts]", "Martial Arts"),
            Filter("genres[Mature]", "Mature"),
            Filter("genres[Mecha]", "Mecha"),
            Filter("genres[Mystery]", "Mystery"),
            Filter("genres[One Shot]", "One Shot"),
            Filter("genres[Psychological]", "Psychological"),
            Filter("genres[Romance]", "Romance"),
            Filter("genres[School Life]", "School Life"),
            Filter("genres[Sci-fi]", "Sci-fi"),
            Filter("genres[Seinen]", "Seinen"),
            Filter("genres[Shoujo]", "Shoujo"),
            Filter("genres[Shoujo Ai]", "Shoujo Ai"),
            Filter("genres[Shounen]", "Shounen"),
            Filter("genres[Shounen Ai]", "Shounen Ai"),
            Filter("genres[Slice of Life]", "Slice of Life"),
            Filter("genres[Sports]", "Sports"),
            Filter("genres[Supernatural]", "Supernatural"),
            Filter("genres[Tragedy]", "Tragedy"),
            Filter("genres[Yaoi]", "Yaoi"),
            Filter("genres[Yuri]", "Yuri")
    )
}
