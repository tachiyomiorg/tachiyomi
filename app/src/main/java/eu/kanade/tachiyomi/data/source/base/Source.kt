package eu.kanade.tachiyomi.data.source.base

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.Page
import rx.Observable

interface Source {

    val id: Int

    val name: String

    fun fetchMangaDetails(manga: Manga): Observable<Manga>

    fun fetchChapters(manga: Manga): Observable<List<Chapter>>

    fun fetchPageList(chapter: Chapter): Observable<List<Page>>

    fun fetchImage(page: Page): Observable<Page>

}