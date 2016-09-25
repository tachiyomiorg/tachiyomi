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
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.MangasPage
import okhttp3.Response
import org.jsoup.nodes.Document
import rx.Observable

class DynastySeries(context: Context, override val id: Int) : DynastyScans(context) {

    override val name = "Dynasty-Series"

    override fun popularMangaInitialUrl() = "$baseUrl/series?view=cover"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/search?q=$query&classes[]=Series&sort="

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        manga.thumbnail_url = baseUrl + document.select("div.span2 > img").attr("src")
        //manga.author = document.select("h2.tag-title > a").first().text()
        //manga.artist = document.select("h2.tag-title > a + a").text()
        parseHeader(document, manga)
        parseGenres(document, manga)
        //manga.status = document.select("h2.tag-title > small").text().orEmpty().let { parseStatus(it) }
        parseDescription(document, manga)
    }

}