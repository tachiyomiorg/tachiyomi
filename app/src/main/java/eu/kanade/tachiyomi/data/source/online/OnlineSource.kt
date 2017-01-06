package eu.kanade.tachiyomi.data.source.online

import android.net.Uri
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.network.asObservableSuccess
import eu.kanade.tachiyomi.data.network.newCallWithProgress
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.model.SChapter
import eu.kanade.tachiyomi.data.source.model.SManga
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

/**
 * A simple implementation for sources from a website.
 */
abstract class OnlineSource : Source {

    /**
     * Network service.
     */
    val network: NetworkHelper by injectLazy()

    /**
     * Chapter cache.
     */
    val chapterCache: ChapterCache by injectLazy()

    /**
     * Preferences helper.
     */
    val preferences: PreferencesHelper by injectLazy()

    /**
     * Base url of the website without the trailing slash, like: http://mysite.com
     */
    abstract val baseUrl: String

    /**
     * An ISO 639-1 compliant language code (two characters in lower case).
     */
    abstract val lang: String

    /**
     * Whether the source has support for latest updates.
     */
    abstract val supportsLatest: Boolean

    /**
     * Id of the source. By default it uses a generated id.
     */
    override val id by lazy {
        val key = "$lang - ${name.toLowerCase()}"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() shl (8 * it) }.reduce(Long::or)
    }

    /**
     * Headers used for requests.
     */
    val headers: Headers by lazy { headersBuilder().build() }

    /**
     * Genre filters.
     */
    val filters by lazy { getFilterList() }

    /**
     * Default network client for doing requests.
     */
    open val client: OkHttpClient
        get() = network.client

    /**
     * Headers builder for requests. Implementations can override this method for custom headers.
     */
    open protected fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
    }

    /**
     * Visible name of the source.
     */
    override fun toString() = "$name (${lang.toUpperCase()})"

    /**
     * Returns an observable containing a page with a list of manga. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     */
    open fun fetchPopularManga(page: Int): Observable<MangasPage> = client
            .newCall(popularMangaRequest(page))
            .asObservableSuccess()
            .map { response ->
                popularMangaParse(response)
            }

    /**
     * Returns the request for the popular manga given the page.
     *
     * @param page the page number to retrieve.
     */
    abstract protected fun popularMangaRequest(page: Int): Request

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    abstract protected fun popularMangaParse(response: Response): MangasPage

    /**
     * Returns an observable containing a page with a list of manga. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     */
    open fun fetchSearchManga(page: Int, query: String, filters: List<Filter<*>>): Observable<MangasPage> = client
            .newCall(searchMangaRequest(page, query, filters))
            .asObservableSuccess()
            .map { response ->
                searchMangaParse(response)
            }

    /**
     * Returns the request for the search manga given the page.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    abstract protected fun searchMangaRequest(page: Int, query: String, filters: List<Filter<*>>): Request

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    abstract protected fun searchMangaParse(response: Response): MangasPage

    /**
     * Returns an observable containing a page with a list of latest manga updates.
     *
     * @param page the page number to retrieve.
     */
    open fun fetchLatestUpdates(page: Int): Observable<MangasPage> = client
            .newCall(latestUpdatesRequest(page))
            .asObservableSuccess()
            .map { response ->
                latestUpdatesParse(response)
            }

    /**
     * Returns the request for latest manga given the page.
     *
     * @param page the page number to retrieve.
     */
    abstract protected fun latestUpdatesRequest(page: Int): Request

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    abstract protected fun latestUpdatesParse(response: Response): MangasPage

    /**
     * Returns an observable with the updated details for a manga. Normally it's not needed to
     * override this method.
     *
     * @param manga the manga to be updated.
     */
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = client
            .newCall(mangaDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }

    /**
     * Returns the request for the details of a manga. Override only if it's needed to change the
     * url, send different headers or request method like POST.
     *
     * @param manga the manga to be updated.
     */
    open fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    /**
     * Parses the response from the site and returns the details of a manga.
     *
     * @param response the response from the site.
     */
    abstract protected fun mangaDetailsParse(response: Response): SManga

    /**
     * Returns an observable with the updated chapter list for a manga. Normally it's not needed to
     * override this method.
     *
     * @param manga the manga to look for chapters.
     */
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = client
            .newCall(chapterListRequest(manga))
            .asObservableSuccess()
            .map { response ->
                chapterListParse(response)
            }

    /**
     * Returns the request for updating the chapter list. Override only if it's needed to override
     * the url, send different headers or request method like POST.
     *
     * @param manga the manga to look for chapters.
     */
    open protected fun chapterListRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    abstract protected fun chapterListParse(response: Response): List<SChapter>

    /**
     * Returns an observable with the page list for a chapter. It tries to return the page list from
     * the local cache, otherwise fallbacks to network calling [fetchPageListFromNetwork].
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    final override fun fetchPageList(chapter: SChapter): Observable<List<Page>> = chapterCache
            .getPageListFromCache(getChapterCacheKey(chapter))
            .onErrorResumeNext { fetchPageListFromNetwork(chapter) }
            .doOnNext { if (it.isEmpty()) throw Exception("Page list is empty") }

    /**
     * Returns an observable with the page list for a chapter. Normally it's not needed to override
     * this method.
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    open fun fetchPageListFromNetwork(chapter: SChapter): Observable<List<Page>> = client
            .newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                pageListParse(response)
            }

    /**
     * Returns the request for getting the page list. Override only if it's needed to override the
     * url, send different headers or request method like POST.
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    open protected fun pageListRequest(chapter: SChapter): Request {
        return GET(baseUrl + chapter.url, headers)
    }

    /**
     * Parses the response from the site and returns a list of pages.
     *
     * @param response the response from the site.
     */
    abstract protected fun pageListParse(response: Response): List<Page>

    /**
     * Returns the key for the page list to be stored in [ChapterCache].
     */
    private fun getChapterCacheKey(chapter: SChapter) = "$id${chapter.url}"

    /**
     * Returns an observable with the page containing the source url of the image. If there's any
     * error, it will return null instead of throwing an exception.
     *
     * @param page the page whose source image has to be fetched.
     */
    open protected fun fetchImageUrl(page: Page): Observable<Page> {
        page.status = Page.LOAD_PAGE
        return client
                .newCall(imageUrlRequest(page))
                .asObservableSuccess()
                .map { imageUrlParse(it) }
                .doOnError { page.status = Page.ERROR }
                .onErrorReturn { null }
                .doOnNext { page.imageUrl = it }
                .map { page }
    }

    /**
     * Returns the request for getting the url to the source image. Override only if it's needed to
     * override the url, send different headers or request method like POST.
     *
     * @param page the chapter whose page list has to be fetched
     */
    open protected fun imageUrlRequest(page: Page): Request {
        return GET(page.url, headers)
    }

    /**
     * Parses the response from the site and returns the absolute url to the source image.
     *
     * @param response the response from the site.
     */
    abstract protected fun imageUrlParse(response: Response): String

    /**
     * Returns an observable of the page with the downloaded image.
     *
     * @param page the page whose source image has to be downloaded.
     */
    final override fun fetchImage(page: Page): Observable<Page> =
            if (page.imageUrl.isNullOrEmpty())
                fetchImageUrl(page).flatMap { getCachedImage(it) }
            else
                getCachedImage(page)

    /**
     * Returns an observable with the response of the source image.
     *
     * @param page the page whose source image has to be downloaded.
     */
    fun imageResponse(page: Page): Observable<Response> = client
            .newCallWithProgress(imageRequest(page), page)
            .asObservableSuccess()

    /**
     * Returns the request for getting the source image. Override only if it's needed to override
     * the url, send different headers or request method like POST.
     *
     * @param page the chapter whose page list has to be fetched
     */
    open protected fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    /**
     * Returns an observable of the page that gets the image from the chapter or fallbacks to
     * network and copies it to the cache calling [cacheImage].
     *
     * @param page the page.
     */
    fun getCachedImage(page: Page): Observable<Page> {
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
    private fun cacheImage(page: Page): Observable<Page> {
        page.status = Page.DOWNLOAD_IMAGE
        return imageResponse(page)
                .doOnNext { chapterCache.putImageToCache(page.imageUrl!!, it) }
                .map { page }
    }


    // Utility methods

    fun fetchAllImageUrlsFromPageList(pages: List<Page>): Observable<Page> = Observable.from(pages)
            .filter { !it.imageUrl.isNullOrEmpty() }
            .mergeWith(fetchRemainingImageUrlsFromPageList(pages))

    fun fetchRemainingImageUrlsFromPageList(pages: List<Page>): Observable<Page> = Observable.from(pages)
            .filter { it.imageUrl.isNullOrEmpty() }
            .concatMap { fetchImageUrl(it) }

    fun savePageList(chapter: SChapter, pages: List<Page>?) {
        if (pages != null) {
            chapterCache.putPageListToCache(getChapterCacheKey(chapter), pages)
        }
    }

    fun SChapter.setUrlWithoutDomain(url: String) {
        this.url = getUrlPath(url)
    }

    fun SManga.setUrlWithoutDomain(url: String) {
        this.url = getUrlPath(url)
    }

    fun getUrlPath(s: String): String {
        try {
            val uri = URI(s)
            var out = uri.path
            if (uri.query != null)
                out += "?" + uri.query
            if (uri.fragment != null)
                out += "#" + uri.fragment
            return out
        } catch (e: URISyntaxException) {
            return s
        }

    }

    /**
     * Called before inserting a new chapter into database. Use it if you need to override chapter
     * fields, like the title or the chapter number. Do not change anything to [manga].
     *
     * @param chapter the chapter to be added.
     * @param manga the manga of the chapter.
     */
    open fun prepareNewChapter(chapter: SChapter, manga: SManga) {
    }

    sealed class Filter<T>(val name: String, var state: T) {
        open class Header(name: String) : Filter<Any>(name, 0)
        abstract class List<V>(name: String, val values: Array<V>, state: Int = 0) : Filter<Int>(name, state)
        abstract class Text(name: String, state: String = "") : Filter<String>(name, state)
        abstract class CheckBox(name: String, state: Boolean = false) : Filter<Boolean>(name, state)
        abstract class TriState(name: String, state: Int = STATE_IGNORE) : Filter<Int>(name, state) {
            fun isIgnored() = state == STATE_IGNORE
            fun isIncluded() = state == STATE_INCLUDE
            fun isExcluded() = state == STATE_EXCLUDE
            companion object {
                const val STATE_IGNORE = 0
                const val STATE_INCLUDE = 1
                const val STATE_EXCLUDE = 2
            }
        }
    }

    open fun getFilterList(): List<Filter<*>> = emptyList()
}
