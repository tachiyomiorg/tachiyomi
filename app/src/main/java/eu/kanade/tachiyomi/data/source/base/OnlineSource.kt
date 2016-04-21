package eu.kanade.tachiyomi.data.source.base

import android.content.Context
import com.bumptech.glide.load.model.LazyHeaders
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.network.get
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import javax.inject.Inject

@Suppress("unused")
abstract class OnlineSource(context: Context) : Source {

    @Inject lateinit var networkService: NetworkHelper

    @Inject lateinit var chapterCache: ChapterCache

    @Inject lateinit var preferences: PreferencesHelper

    abstract val baseUrl: String

    abstract val lang: Language

    val headers by lazy { headersBuilder().build() }

    open val client: OkHttpClient
        get() = networkService.defaultClient

    init {
        App.get(context).component.inject(this)
    }

    // Headers for requests

    open fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
    }

    open fun glideHeadersBuilder() = LazyHeaders.Builder().apply {
        for ((key, value) in headers.toMultimap()) {
            addHeader(key, value[0])
        }
    }

    override fun toString() = "$name (${lang.code})"

    // Login source

    open fun isLoginRequired() = false

    open fun isLogged(): Boolean = throw Exception("Not implemented")

    open fun login(username: String, password: String): Observable<Boolean> = throw Exception("Not implemented")

    open fun isAuthenticationSuccessful(response: Response): Boolean = throw Exception("Not implemented")

    // Popular manga

    abstract fun getInitialPopularMangaUrl(): String

    open fun popularMangaRequest(page: MangasPage): Request {
        if (page.page == 1) {
            page.url = getInitialPopularMangaUrl()
        }
        return get(page.url, headers)
    }

    open fun fetchPopularManga(page: MangasPage) = networkService.requestBody(popularMangaRequest(page), client)
            .map { html ->
                page.apply {
                    mangas = mutableListOf<Manga>()
                    parseHtmlToPopularManga(html, this)
                }
            }

    abstract fun parseHtmlToPopularManga(html: String, page: MangasPage)

    // Search manga

    abstract fun getInitialSearchUrl(query: String): String

    open fun searchMangaRequest(page: MangasPage, query: String): Request {
        if (page.page == 1) {
            page.url = getInitialSearchUrl(query)
        }
        return get(page.url, headers)
    }

    open fun searchManga(page: MangasPage, query: String) = networkService.requestBody(searchMangaRequest(page, query), client)
            .map { html ->
                page.apply {
                    mangas = mutableListOf<Manga>()
                    parseSearchFromHtml(html, this, query)
                }
            }

    abstract fun parseSearchFromHtml(html: String, page: MangasPage, query: String)

    // Manga details

    open fun mangaDetailsRequest(manga: Manga) = get(baseUrl + manga.url, headers)

    override fun fetchMangaDetails(manga: Manga) = networkService.requestBody(mangaDetailsRequest(manga))
            .map { html ->
                Manga.create(manga.url, id).apply {
                    parseHtmlToManga(html, this)
                    initialized = true
                }
            }

    abstract fun parseHtmlToManga(html: String, manga: Manga)

    // Chapter list

    open fun chapterListRequest(manga: Manga) = get(baseUrl + manga.url, headers)
    
    override fun fetchChapters(manga: Manga): Observable<List<Chapter>> = networkService.requestBody(chapterListRequest(manga))
            .map { html ->
                mutableListOf<Chapter>().apply {
                    parseHtmlToChapters(html, this)
                    if (isEmpty()) {
                        throw Exception("No chapters found")
                    }
                }
            }

    abstract fun parseHtmlToChapters(html: String, chapters: MutableList<Chapter>)

    // Page list

    open fun pageListRequest(chapter: Chapter) = get(baseUrl + chapter.url, headers)

    override fun fetchPageList(chapter: Chapter) =
            chapterCache.getPageListFromCache(getChapterCacheKey(chapter))
                    .onErrorResumeNext { fetchPageListFromNetwork(chapter) }

    open fun fetchPageListFromNetwork(chapter: Chapter) = networkService.request(pageListRequest(chapter))
            .map { response ->
                val html = response.body().string()

                mutableListOf<Page>().apply {
                    parseHtmlToPages(response, html, this)
                    if (isEmpty()) {
                        throw Exception("Page list is empty")
                    }
                }
            }

    abstract fun parseHtmlToPages(response: Response, html: String, pages: MutableList<Page>)

    fun getChapterCacheKey(chapter: Chapter) = "$id${chapter.url}"

    // A page

    open fun imageUrlRequest(page: Page) = get(page.url, headers)

    fun fetchImageUrl(page: Page): Observable<Page> {
        page.status = Page.LOAD_PAGE
        return networkService.requestBody(imageUrlRequest(page))
                .map { parseHtmlToImageUrl(it) }
                .doOnError { page.status = Page.ERROR }
                .onErrorReturn { null }
                .map {
                    page.apply { imageUrl = it }
                }
    }

    abstract fun parseHtmlToImageUrl(html: String): String

    // An image

    open fun imageRequest(page: Page) = get(page.imageUrl, headers)

    fun imageResponse(page: Page): Observable<Response> {
        return networkService.requestBodyProgress(imageRequest(page), page)
                .doOnNext {
                    if (!it.isSuccessful) {
                        it.body().close()
                        throw RuntimeException("Not a valid response")
                    }
                }
    }

    open fun getCachedImage(page: Page): Observable<Page> {
        val pageObservable = Observable.just(page)
        if (page.imageUrl.isNullOrEmpty())
            return pageObservable

        return pageObservable
                .flatMap {
                    if (!chapterCache.isImageInCache(page.imageUrl)) {
                        cacheImage(page)
                    } else {
                        Observable.just(page)
                    }
                }
                .doOnNext {
                    page.imagePath = chapterCache.getImagePath(page.imageUrl)
                    page.status = Page.READY
                }
                .onErrorReturn {
                    page.apply { status = Page.ERROR }
                }
    }

    private fun cacheImage(page: Page): Observable<Page> {
        page.status = Page.DOWNLOAD_IMAGE
        return imageResponse(page)
                .map {
                    page.apply { chapterCache.putImageToCache(imageUrl, it, preferences.reencodeImage()) }
                }
    }

    override fun fetchImage(page: Page): Observable<Page> {
        if (page.imageUrl.isNullOrEmpty()) {
            return fetchImageUrl(page).flatMap { getCachedImage(it) }
        } else {
            return getCachedImage(page)
        }
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

    // Overridable method to allow custom parsing.
    open fun parseChapterNumber(chapter: Chapter) {

    }

}

