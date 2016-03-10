package eu.kanade.tachiyomi.ui.catalogue

import android.os.Bundle
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.base.Source
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.RxPager
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

/**
 * Presenter of [CatalogueFragment].
 */
class CataloguePresenter : BasePresenter<CatalogueFragment>() {

    /**
     * Source manager.
     */
    @Inject lateinit var sourceManager: SourceManager

    /**
     * Database.
     */
    @Inject lateinit var db: DatabaseHelper

    /**
     * Cover cache.
     */
    @Inject lateinit var coverCache: CoverCache

    /**
     * Preferences.
     */
    @Inject lateinit var prefs: PreferencesHelper

    /**
     * Enabled sources.
     */
    val sources by lazy { getEnabledSources() }

    /**
     * Active source.
     */
    lateinit var source: Source
        private set

    /**
     * Query from the view.
     */
    private var query: String? = null

    /**
     * Pager containing a list of manga results.
     */
    private lateinit var pager: RxPager<Manga>

    /**
     * Last fetched page from network.
     */
    private var lastMangasPage: MangasPage? = null

    /**
     * Subject that initializes a list of manga.
     */
    private val mangaDetailSubject = PublishSubject.create<List<Manga>>()

    /**
     * Whether the view is in list mode or not.
     */
    var isListMode: Boolean = false
        private set

    companion object {
        /**
         * Id of the restartable that delivers a list of manga from network.
         */
        const val GET_MANGA_LIST = 1

        /**
         * Id of the restartable that requests the list of manga from network.
         */
        const val GET_MANGA_PAGE = 2

        /**
         * Id of the restartable that initializes the details of a manga.
         */
        const val GET_MANGA_DETAIL = 3

        /**
         * Key to save and restore [source] from a [Bundle].
         */
        const val ACTIVE_SOURCE_KEY = "active_source"
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (savedState != null) {
            source = sourceManager.get(savedState.getInt(ACTIVE_SOURCE_KEY))!!
        }

        pager = RxPager()

        startableReplay(GET_MANGA_LIST,
                { pager.results() },
                { view, pair -> view.onAddPage(pair.first, pair.second) })

        startableFirst(GET_MANGA_PAGE,
                { pager.request { page -> getMangasPageObservable(page + 1) } },
                { view, next -> },
                { view, error -> view.onAddPageError(error) })

        startableLatestCache(GET_MANGA_DETAIL,
                { mangaDetailSubject.observeOn(Schedulers.io())
                        .flatMap { Observable.from(it) }
                        .filter { !it.initialized }
                        .concatMap { getMangaDetailsObservable(it) }
                        .onBackpressureBuffer()
                        .observeOn(AndroidSchedulers.mainThread()) },
                { view, manga -> view.onMangaInitialized(manga) },
                { view, error -> Timber.e(error.message) })

        add(prefs.catalogueAsList().asObservable()
                .subscribe { setDisplayMode(it) })
    }

    override fun onSave(state: Bundle) {
        state.putInt(ACTIVE_SOURCE_KEY, source.id)
        super.onSave(state)
    }

    /**
     * Sets the display mode.
     *
     * @param asList whether the current mode is in list or not.
     */
    private fun setDisplayMode(asList: Boolean) {
        isListMode = asList
        if (asList) {
            stop(GET_MANGA_DETAIL)
        } else {
            start(GET_MANGA_DETAIL)
        }
    }

    /**
     * Starts the request with the given source.
     *
     * @param source the active source.
     */
    fun startRequesting(source: Source) {
        this.source = source
        restartRequest(null)
    }

    /**
     * Restarts the request for the active source with a query.
     *
     * @param query a query, or null if searching popular manga.
     */
    fun restartRequest(query: String?) {
        this.query = query
        stop(GET_MANGA_PAGE)
        lastMangasPage = null

        if (!isListMode) {
            start(GET_MANGA_DETAIL)
        }
        start(GET_MANGA_LIST)
        start(GET_MANGA_PAGE)
    }

    /**
     * Requests the next page for the active pager.
     */
    fun requestNext() {
        if (hasNextPage()) {
            start(GET_MANGA_PAGE)
        }
    }

    /**
     * Retry a failed request.
     */
    fun retryRequest() {
        start(GET_MANGA_PAGE)
    }

    /**
     * Returns the observable of the network request for a page.
     *
     * @param page the page number to request.
     * @return an observable of the network request.
     */
    private fun getMangasPageObservable(page: Int): Observable<List<Manga>> {
        val nextMangasPage = MangasPage(page)
        if (page != 1) {
            nextMangasPage.url = lastMangasPage!!.nextPageUrl
        }

        val obs = if (query.isNullOrEmpty())
            source.pullPopularMangasFromNetwork(nextMangasPage)
        else
            source.searchMangasFromNetwork(nextMangasPage, query)

        return obs.subscribeOn(Schedulers.io())
                .doOnNext { lastMangasPage = it }
                .flatMap { Observable.from(it.mangas) }
                .map { networkToLocalManga(it) }
                .toList()
                .doOnNext { initializeMangas(it) }
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Returns a manga from the database for the given manga from network. It creates a new entry
     * if the manga is not yet in the database.
     *
     * @param networkManga the manga from network.
     * @return a manga from the database.
     */
    private fun networkToLocalManga(networkManga: Manga): Manga {
        var localManga = db.getManga(networkManga.url, source.id).executeAsBlocking()
        if (localManga == null) {
            val result = db.insertManga(networkManga).executeAsBlocking()
            networkManga.id = result.insertedId()
            localManga = networkManga
        }
        return localManga
    }

    /**
     * Initialize a list of manga.
     *
     * @param mangas the list of manga to initialize.
     */
    fun initializeMangas(mangas: List<Manga>) {
        mangaDetailSubject.onNext(mangas)
    }

    /**
     * Returns an observable of manga that initializes the given manga.
     *
     * @param manga the manga to initialize.
     * @return an observable of the manga to initialize
     */
    private fun getMangaDetailsObservable(manga: Manga): Observable<Manga> {
        return source.pullMangaFromNetwork(manga.url)
                .flatMap { networkManga ->
                    manga.copyFrom(networkManga)
                    db.insertManga(manga).executeAsBlocking()
                    Observable.just(manga)
                }
                .onErrorResumeNext { Observable.just(manga) }
    }

    /**
     * Returns true if the last fetched page has a next page.
     */
    fun hasNextPage(): Boolean {
        return lastMangasPage?.nextPageUrl != null
    }

    /**
     * Gets the last used source from preferences, or the first valid source.
     *
     * @return the index of the last used source.
     */
    fun getLastUsedSourceIndex(): Int {
        val index = prefs.lastUsedCatalogueSource().get() ?: -1
        if (index < 0 || index >= sources.size || !isValidSource(sources[index])) {
            return findFirstValidSource()
        }
        return index
    }

    /**
     * Checks if the given source is valid.
     *
     * @param source the source to check.
     * @return true if the source is valid, false otherwise.
     */
    fun isValidSource(source: Source): Boolean = with(source) {
        if (!isLoginRequired || isLogged)
            return true

        prefs.getSourceUsername(this) != "" && prefs.getSourcePassword(this) != ""
    }

    /**
     * Finds the first valid source.
     *
     * @return the index of the first valid source.
     */
    fun findFirstValidSource(): Int {
        return sources.indexOfFirst { isValidSource(it) }
    }

    /**
     * Sets the enabled source.
     *
     * @param index the index of the source in [sources].
     */
    fun setEnabledSource(index: Int) {
        prefs.lastUsedCatalogueSource().set(index)
    }

    /**
     * Returns a list of enabled sources ordered by language and name.
     */
    private fun getEnabledSources(): List<Source> {
        val languages = prefs.enabledLanguages().getOrDefault()

        // Ensure at least one language
        if (languages.isEmpty()) {
            languages.add("EN")
        }

        return sourceManager.getSources()
                .filter { it.lang.code in languages }
                .sortedBy { "(${it.lang.code}) ${it.name}" }
    }

    /**
     * Adds or removes a manga from the library.
     *
     * @param manga the manga to update.
     */
    fun changeMangaFavorite(manga: Manga) {
        manga.favorite = !manga.favorite
        db.insertManga(manga).executeAsBlocking()
    }

    /**
     * Changes the active display mode.
     */
    fun swapDisplayMode() {
        prefs.catalogueAsList().set(!isListMode)
    }

}
