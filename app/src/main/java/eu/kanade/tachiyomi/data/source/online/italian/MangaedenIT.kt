package eu.kanade.tachiyomi.data.source.online.italian

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.IT
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.online.multi.Mangaeden
import org.jsoup.nodes.*
import java.text.SimpleDateFormat
import java.util.*

class MangaedenIT(context: Context, override val id: Int) : Mangaeden(context){

    override val lang: Language = IT

    override val langcode = lang.code.toLowerCase()

    override fun popularMangaNextPageSelector() = "div.pagination.pagination_bottom > a:has(span:contains(Avanti))"

    override fun searchMangaNextPageSelector() = "div.pagination.pagination_bottom > a:has(span:contains(Avanti))"

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val ielement = document.select("div#mangaPage")
        val info = getTextNodes(ielement.select("div#rightContent").first().childNode(3).childNodes())

        manga.thumbnail_url = "http:" + ielement.select("div#rightContent div.mangaImage2 > img").attr("src")
        manga.author = info[info.indexOf("Autore") + 1]
        manga.artist = info[info.indexOf("Artista") + 1]
        val s = StringBuilder()
        for (i in info.indexOf("Genere") + 1 .. info.indexOf("Tipo") - 1) {
            s.append(info[i])
        }
        manga.genre = s.toString()
        manga.status = info[info.indexOf("Stato") + 1].let { parseStatus(it) }
        manga.description = ielement.select("h2#mangaDescription").text()
    }

    override fun parseStatus(status: String) = when {
        status.contains("En curso") -> Manga.ONGOING
        status.contains("Completado") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("td > a.chapterLink")

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.select("b").text()
        chapter.date_upload = element.select("td.chapterDate").text()?.let {
            SimpleDateFormat("dd MMM yyyy", Locale(langcode)).parse(it).time
        } ?: 0
    }

}
