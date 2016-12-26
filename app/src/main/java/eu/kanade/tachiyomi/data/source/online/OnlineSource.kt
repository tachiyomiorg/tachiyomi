package eu.kanade.tachiyomi.data.source.online

import android.net.Uri
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.network.asObservableSuccess
import eu.kanade.tachiyomi.data.network.newCallWithProgress
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.util.UrlUtil
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy

/**
 * A simple implementation for sources from a website.
 */
abstract class OnlineSource() : Source {

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
    abstract val supportsLatest : Boolean

    /**
     * Headers used for requests.
     */
    val headers by lazy { headersBuilder().build() }

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
     * @param page the page object where the information will be saved, like the list of manga,
     *             the current page and the next page url.
     */
    open fun fetchPopularManga(page: MangasPage): Observable<MangasPage> = client
            .newCall(popularMangaRequest(page))
            .asObservableSuccess()
            .map { response ->
                popularMangaParse(response, page)
                page
            }

    /**
     * Returns the request for the popular manga given the page. Override only if it's needed to
     * send different headers or request method like POST.
     *
     * @param page the page object.
     */
    open protected fun popularMangaRequest(page: MangasPage): Request {
        if (page.page == 1) {
            page.url = popularMangaInitialUrl()
        }
        return GET(page.url, headers)
    }

    /**
     * Returns the absolute url of the first page to popular manga.
     */
    abstract protected fun popularMangaInitialUrl(): String

    /**
     * Parse the response from the site. It should add a list of manga and the absolute url to the
     * next page (if it has a next one) to [page].
     *
     * @param response the response from the site.
     * @param page the page object to be filled.
     */
    abstract protected fun popularMangaParse(response: Response, page: MangasPage)

    /**
     * Returns an observable containing a page with a list of manga. Normally it's not needed to
     * override this method.
     *
     * @param page the page object where the information will be saved, like the list of manga,
     *             the current page and the next page url.
     * @param query the search query.
     */
    open fun fetchSearchManga(page: MangasPage, query: String, filters: List<Filter>): Observable<MangasPage> = client
            .newCall(searchMangaRequest(page, query, filters))
            .asObservableSuccess()
            .map { response ->
                searchMangaParse(response, page, query, filters)
                page
            }

    /**
     * Returns the request for the search manga given the page. Override only if it's needed to
     * send different headers or request method like POST.
     *
     * @param page the page object.
     * @param query the search query.
     */
    open protected fun searchMangaRequest(page: MangasPage, query: String, filters: List<Filter>): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query, filters)
        }
        return GET(page.url, headers)
    }

    /**
     * Returns the absolute url of the first page to popular manga.
     *
     * @param query the search query.
     */
    abstract protected fun searchMangaInitialUrl(query: String, filters: List<Filter>): String

    /**
     * Parse the response from the site. It should add a list of manga and the absolute url to the
     * next page (if it has a next one) to [page].
     *
     * @param response the response from the site.
     * @param page the page object to be filled.
     * @param query the search query.
     */
    abstract protected fun searchMangaParse(response: Response, page: MangasPage, query: String, filters: List<Filter>)

    /**
     * Returns an observable containing a page with a list of latest manga.
     */
    open fun fetchLatestUpdates(page: MangasPage): Observable<MangasPage> = client
            .newCall(latestUpdatesRequest(page))
            .asObservableSuccess()
            .map { response ->
                latestUpdatesParse(response, page)
                page
            }

    /**
     * Returns the request for latest manga given the page.
     */
    open protected fun latestUpdatesRequest(page: MangasPage): Request {
        if (page.page == 1) {
            page.url = latestUpdatesInitialUrl()
        }
        return GET(page.url, headers)
    }

    /**
     * Returns the absolute url of the first page to latest manga.
     */
    abstract protected fun latestUpdatesInitialUrl(): String

    /**
     * Same as [popularMangaParse], but for latest manga.
     */
    abstract protected fun latestUpdatesParse(response: Response, page: MangasPage)

    /**
     * Returns an observable with the updated details for a manga. Normally it's not needed to
     * override this method.
     *
     * @param manga the manga to be updated.
     */
    override fun fetchMangaDetails(manga: Manga): Observable<Manga> = client
            .newCall(mangaDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                Manga.create(manga.url, id).apply {
                    mangaDetailsParse(response, this)
                    initialized = true
                }
            }

    /**
     * Returns the request for updating a manga. Override only if it's needed to override the url,
     * send different headers or request method like POST.
     *
     * @param manga the manga to be updated.
     */
    open fun mangaDetailsRequest(manga: Manga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    /**
     * Parse the response from the site. It should fill [manga].
     *
     * @param response the response from the site.
     * @param manga the manga whose fields have to be filled.
     */
    abstract protected fun mangaDetailsParse(response: Response, manga: Manga)

    /**
     * Returns an observable with the updated chapter list for a manga. Normally it's not needed to
     * override this method.
     *
     * @param manga the manga to look for chapters.
     */
    override fun fetchChapterList(manga: Manga): Observable<List<Chapter>> = client
            .newCall(chapterListRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mutableListOf<Chapter>().apply {
                    chapterListParse(response, this)
                    if (isEmpty()) {
                        throw Exception("No chapters found")
                    }
                }
            }

    /**
     * Returns the request for updating the chapter list. Override only if it's needed to override
     * the url, send different headers or request method like POST.
     *
     * @param manga the manga to look for chapters.
     */
    open protected fun chapterListRequest(manga: Manga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    /**
     * Parse the response from the site. It should fill [chapters].
     *
     * @param response the response from the site.
     * @param chapters the chapter list to be filled.
     */
    abstract protected fun chapterListParse(response: Response, chapters: MutableList<Chapter>)

    /**
     * Returns an observable with the page list for a chapter. It tries to return the page list from
     * the local cache, otherwise fallbacks to network calling [fetchPageListFromNetwork].
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    final override fun fetchPageList(chapter: Chapter): Observable<List<Page>> = chapterCache
            .getPageListFromCache(getChapterCacheKey(chapter))
            .onErrorResumeNext { fetchPageListFromNetwork(chapter) }

    /**
     * Returns an observable with the page list for a chapter. Normally it's not needed to override
     * this method.
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    open fun fetchPageListFromNetwork(chapter: Chapter): Observable<List<Page>> = client
            .newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                mutableListOf<Page>().apply {
                    pageListParse(response, this)
                    if (isEmpty()) {
                        throw Exception("Page list is empty")
                    }
                }
            }

    /**
     * Returns the request for getting the page list. Override only if it's needed to override the
     * url, send different headers or request method like POST.
     *
     * @param chapter the chapter whose page list has to be fetched
     */
    open protected fun pageListRequest(chapter: Chapter): Request {
        return GET(baseUrl + chapter.url, headers)
    }

    /**
     * Parse the response from the site. It should fill [pages].
     *
     * @param response the response from the site.
     * @param pages the page list to be filled.
     */
    abstract protected fun pageListParse(response: Response, pages: MutableList<Page>)

    /**
     * Returns the key for the page list to be stored in [ChapterCache].
     */
    private fun getChapterCacheKey(chapter: Chapter) = "$id${chapter.url}"

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
     * Parse the response from the site. It should return the absolute url to the source image.
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

    fun fetchAllImageUrlsFromPageList(pages: List<Page>) = Observable.from(pages)
            .filter { !it.imageUrl.isNullOrEmpty() }
            .mergeWith(fetchRemainingImageUrlsFromPageList(pages))

    fun fetchRemainingImageUrlsFromPageList(pages: List<Page>) = Observable.from(pages)
            .filter { it.imageUrl.isNullOrEmpty() }
            .concatMap { fetchImageUrl(it) }

    fun savePageList(chapter: Chapter, pages: List<Page>?) {
        if (pages != null) {
            chapterCache.putPageListToCache(getChapterCacheKey(chapter), pages)
        }
    }

    fun Chapter.setUrlWithoutDomain(url: String) {
        this.url = UrlUtil.getPath(url)
    }

    fun Manga.setUrlWithoutDomain(url: String) {
        this.url = UrlUtil.getPath(url)
    }


    /**
     * Called before inserting a new chapter into database. Use it if you need to override chapter
     * fields, like the title or the chapter number. Do not change anything to [manga].
     *
     * @param chapter the chapter to be added.
     * @param manga the manga of the chapter.
     */
    open fun prepareNewChapter(chapter: Chapter, manga: Manga) {

    }

    data class Filter(val id: String, val name: String)

    open fun getFilterList(): List<Filter> = emptyList()
}
