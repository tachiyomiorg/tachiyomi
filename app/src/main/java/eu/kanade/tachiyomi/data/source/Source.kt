package eu.kanade.tachiyomi.data.source

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.Page
import rx.Observable

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc...
 */
interface Source {

    /**
     * Id for the source. Must be unique.
     */
    val id: Int

    /**
     * Name of the source.
     */
    val name: String

    /**
     * Returns an observable with the updated details for a manga.
     *
     * @param manga the manga to update.
     */
    fun fetchMangaDetails(manga: Manga): Observable<Manga>

    /**
     * Returns an observable with all the available chapters for a manga.
     *
     * @param manga the manga to update.
     */
    fun fetchChapterList(manga: Manga): Observable<List<Chapter>>

    /**
     * Returns an observable with the list of pages a chapter has.
     *
     * @param chapter the chapter.
     */
    fun fetchPageList(chapter: Chapter): Observable<List<Page>>

    /**
     * Returns an observable with the path of the image.
     *
     * @param page the page.
     */
    fun fetchImage(page: Page): Observable<Page>
}