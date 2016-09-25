/*
 * This file is part of Kensaku.
 *
 * Kensaku is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kensaku is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kensaku.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.kanade.tachiyomi.data.source.online.english.dynasty

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class DynastyDoujins(context: Context, override val id: Int) : DynastyScans(context) {

    override val name = "Dynasty-Doujins"

    override fun popularMangaInitialUrl() = "$baseUrl/doujins?view=cover"

    override fun popularMangaParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(popularMangaSelector())) {
            Manga.create(id).apply {
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }
    }

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/search?q=$query&classes[]=Doujin&sort="

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        super.mangaDetailsParse(document, manga)
        parseThumbnail(manga)
        manga.author = ".."
        manga.status = Manga.UNKNOWN
        parseGenres(document, manga)
    }

    override fun chapterListSelector() = "div.span9 > dl.chapter-list > dd"

}
