package eu.kanade.tachiyomi.data.source.newbase

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.Page
import rx.Observable

abstract class SourceKt {

    var id = 0

    abstract val name: String

    abstract fun fetchMangaDetails(manga: Manga): Observable<Manga>

    abstract fun fetchChapters(manga: Manga): Observable<List<Chapter>>

    abstract fun fetchPageList(chapter: Chapter): Observable<List<Page>>

    abstract fun fetchImage(page: Page): Observable<Page>

}