package eu.kanade.tachiyomi.source.online

import android.net.Uri
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.model.Page
import rx.Observable
import uy.kohesive.injekt.injectLazy


// TODO: this should be handled with a different approach.

/**
 * Chapter cache.
 */
private val chapterCache: ChapterCache by injectLazy()

/**
 * Returns an observable with the page list for a chapter. It tries to return the page list from
 * the local cache, otherwise fallbacks to network.
 *
 * @param chapter the chapter whose page list has to be fetched.
 */
fun HttpSource.fetchPageListFromCacheThenNet(chapter: Chapter): Observable<List<Page>> {
    return chapterCache
            .getPageListFromCache(chapter)
            .onErrorResumeNext { fetchPageList(chapter) }
}

/**
 * Returns an observable of the page with the downloaded image.
 *
 * @param page the page whose source image has to be downloaded.
 */
fun HttpSource.fetchImageFromCacheThenNet(page: Page): Observable<Page> {
    return if (page.imageUrl.isNullOrEmpty())
        getImageUrl(page).flatMap { getCachedImage(it) }
    else
        getCachedImage(page)
}

fun HttpSource.getImageUrl(page: Page): Observable<Page> {
    page.status = Page.LOAD_PAGE
    return fetchImageUrl(page)
            .doOnError { page.status = Page.ERROR }
            .onErrorReturn { null }
            .doOnNext { page.imageUrl = it }
            .map { page }
}

/**
 * Returns an observable of the page that gets the image from the chapter or fallbacks to
 * network and copies it to the cache calling [cacheImage].
 *
 * @param page the page.
 */
fun HttpSource.getCachedImage(page: Page): Observable<Page> {
    val imageUrl = page.imageUrl ?: return Observable.just(page)

    return Observable.just(page)
            .flatMap {
                if (!chapterCache.isImageInCache(imageUrl)) {
                    cacheImage(page)
                } else {
                    Observable.just(page)
                }
            }
            .doOnNext {
                page.uri = Uri.fromFile(chapterCache.getImageFile(imageUrl))
                page.status = Page.READY
            }
            .doOnError { page.status = Page.ERROR }
            .onErrorReturn { page }
}

/**
 * Returns an observable of the page that downloads the image to [ChapterCache].
 *
 * @param page the page.
 */
private fun HttpSource.cacheImage(page: Page): Observable<Page> {
    page.status = Page.DOWNLOAD_IMAGE
    return fetchImage(page)
            .doOnNext { chapterCache.putImageToCache(page.imageUrl!!, it) }
            .map { page }
}

fun HttpSource.fetchAllImageUrlsFromPageList(pages: List<Page>): Observable<Page> {
    return Observable.from(pages)
            .filter { !it.imageUrl.isNullOrEmpty() }
            .mergeWith(fetchRemainingImageUrlsFromPageList(pages))
}

fun HttpSource.fetchRemainingImageUrlsFromPageList(pages: List<Page>): Observable<Page> {
    return Observable.from(pages)
            .filter { it.imageUrl.isNullOrEmpty() }
            .concatMap { getImageUrl(it) }
}
