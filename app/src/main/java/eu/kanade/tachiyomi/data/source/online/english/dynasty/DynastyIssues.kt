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
import okhttp3.Response
import org.jsoup.nodes.Document

class DynastyIssues(context: Context, override val id: Int) : DynastyScans(context) {

    override val name = "Dynasty-Issues"

    override fun popularMangaInitialUrl() = "$baseUrl/issues?view=cover"

    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) =
            "$baseUrl/search?q=$query&classes[]=Issue&sort="

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        manga.thumbnail_url = baseUrl + document.select("div.span2 > img").attr("src")
        parseHeader(document, manga)
        parseGenres(document, manga)
        parseDescription(document, manga)
    }

}
