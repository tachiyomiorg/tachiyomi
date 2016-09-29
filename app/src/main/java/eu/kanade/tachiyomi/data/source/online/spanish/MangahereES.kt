package eu.kanade.tachiyomi.data.source.online.spanish

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.ES
import eu.kanade.tachiyomi.data.source.online.multi.Mangahere
import org.jsoup.nodes.Document

class MangahereES(context: Context, override val id: Int) : Mangahere(context) {

    override val lang = ES

    override val baseUrl: String = "http://${lang.code}.mangahere.co"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/site/search?name=$query"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val detailElement = document.select(".manga_detail_top").first()
        val infoElement = detailElement.select(".detail_topText").first()

        manga.thumbnail_url = detailElement.childNode(1).attr("src")
        manga.author = infoElement.select("li:eq(4) > a").text()
        manga.genre = infoElement.select("li:eq(3) > a").text().substringAfter(':')
        manga.description = infoElement.select("#show").first()?.text()?.substringBeforeLast("Show less")
        manga.status = infoElement.select("li:eq(5)").first()?.text().orEmpty().let { parseStatus(it) }
    }

    override fun parseStatus(status: String) = when {
        status.contains("En desarrollo") -> Manga.ONGOING
        status.contains("Terminado") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

}