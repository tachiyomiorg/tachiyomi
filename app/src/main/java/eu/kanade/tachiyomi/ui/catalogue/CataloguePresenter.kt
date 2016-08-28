package eu.kanade.tachiyomi.ui.catalogue

import android.os.Bundle
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.online.LoginSource
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of [CatalogueFragment].
 */
class CataloguePresenter : BasePresenter<CatalogueFragment>() {

    /**
     * Source manager.
     */
    val sourceManager: SourceManager by injectLazy()

    /**
     * Database.
     */
    val db: DatabaseHelper by injectLazy()

    /**
     * Preferences.
     */
    val prefs: PreferencesHelper by injectLazy()

    /**
     * Cover cache.
     */
    val coverCache: CoverCache by injectLazy()

    /**
     * Enabled sources.
     */
    val sources by lazy { getEnabledSources() }

    /**
     * Active source.
     */
    lateinit var source: OnlineSource
        private set

    /**
     * Query from the view.
     */
    var query = ""
        private set

    /**
     * Active filters.
     */
    var filters: List<OnlineSource.Filter> = emptyList()

    /**
     * Pager containing a list of manga results.
     */
    private lateinit var pager: CataloguePager

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
         * Id of the restartable that delivers a list of manga.
         */
        const val PAGER = 1

        /**
         * Id of the restartable that requests a page of manga from network.
         */
        const val REQUEST_PAGE = 2

        /**
         * Id of the restartable that initializes the details of manga.
         */
        const val GET_MANGA_DETAILS = 3

    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        source = getLastUsedSource()

        if (savedState != null) {
            query = savedState.getString(CataloguePresenter::query.name, "")
        }

        pager = CataloguePager(source, query, filters)

        startableLatestCache(GET_MANGA_DETAILS,
                {
                    mangaDetailSubject.observeOn(Schedulers.io())
                            .flatMap { Observable.from(it) }
                            .filter { !it.initialized }
                            .concatMap { getMangaDetailsObservable(it) }
                            .onBackpressureBuffer()
                            .observeOn(AndroidSchedulers.mainThread())
                },
                { view, manga -> view.onMangaInitialized(manga) },
                { view, error -> Timber.e(error.message) })

        add(prefs.catalogueAsList().asObservable()
                .subscribe { setDisplayMode(it) })

        startableReplay(PAGER,
                { pager.results() },
                { view, page -> view.onAddPage(page.page, page.mangas) })

        startableFirst(REQUEST_PAGE,
                { pager.requestNext { getPageTransformer(it) } },
                { view, next -> },
                { view, error -> view.onAddPageError(error) })

        start(PAGER)
        start(REQUEST_PAGE)
    }

    override fun onSave(state: Bundle) {
        state.putString(CataloguePresenter::query.name, query)
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
            stop(GET_MANGA_DETAILS)
        } else {
            start(GET_MANGA_DETAILS)
        }
    }

    /**
     * Sets the active source and restarts the pager.
     *
     * @param source the new active source.
     */
    fun setActiveSource(source: OnlineSource) {
        prefs.lastUsedCatalogueSource().set(source.id)
        this.source = source
        filters = emptyList()

        restartPager()
    }

    /**
     * Restarts the request for the active source.
     *
     * @param query the query, or null if searching popular manga.
     */
    fun restartPager(query: String = "") {
        this.query = query
        stop(REQUEST_PAGE)
        pager = CataloguePager(source, query, filters)

        if (!isListMode) {
            start(GET_MANGA_DETAILS)
        }
        start(PAGER)
        start(REQUEST_PAGE)
    }

    /**
     * Requests the next page for the active pager.
     */
    fun requestNext() {
        if (hasNextPage()) {
            start(REQUEST_PAGE)
        }
    }

    /**
     * Returns true if the last fetched page has a next page.
     */
    fun hasNextPage(): Boolean {
        return pager.hasNextPage()
    }

    /**
     * Retries the current request that failed.
     */
    fun retryPage() {
        start(REQUEST_PAGE)
    }

    /**
     * Returns the function to apply to the observable of the list of manga from the source.
     *
     * @param observable the observable from the source.
     * @return the function to apply.
     */
    fun getPageTransformer(observable: Observable<MangasPage>): Observable<MangasPage> {
        return observable.subscribeOn(Schedulers.io())
                .doOnNext { it.mangas.replace { networkToLocalManga(it) } }
                .doOnNext { initializeMangas(it.mangas) }
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Replaces an object in the list with another.
     */
    fun <T> MutableList<T>.replace(block: (T) -> T) {
        forEachIndexed { i, obj ->
            set(i, block(obj))
        }
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
        return source.fetchMangaDetails(manga)
                .flatMap { networkManga ->
                    manga.copyFrom(networkManga)
                    db.insertManga(manga).executeAsBlocking()
                    Observable.just(manga)
                }
                .onErrorResumeNext { Observable.just(manga) }
    }

    /**
     * Returns the last used source from preferences or the first valid source.
     *
     * @return a source.
     */
    fun getLastUsedSource(): OnlineSource {
        val id = prefs.lastUsedCatalogueSource().get() ?: -1
        val source = sourceManager.get(id)
        if (!isValidSource(source)) {
            return findFirstValidSource()
        }
        return source as OnlineSource
    }

    /**
     * Checks if the given source is valid.
     *
     * @param source the source to check.
     * @return true if the source is valid, false otherwise.
     */
    fun isValidSource(source: Source?): Boolean {
        if (source == null) return false

        if (source is LoginSource) {
            return source.isLogged() ||
                    (prefs.sourceUsername(source) != "" && prefs.sourcePassword(source) != "")
        }
        return true
    }

    /**
     * Finds the first valid source.
     *
     * @return the index of the first valid source.
     */
    fun findFirstValidSource(): OnlineSource {
        return sources.first { isValidSource(it) }
    }

    /**
     * Returns a list of enabled sources ordered by language and name.
     */
    private fun getEnabledSources(): List<OnlineSource> {
        val languages = prefs.enabledLanguages().getOrDefault()

        // Ensure at least one language
        if (languages.isEmpty()) {
            languages.add(EN.code)
        }

        return sourceManager.getOnlineSources()
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
        if (!manga.favorite) {
            coverCache.deleteFromCache(manga.thumbnail_url)
        }
        db.insertManga(manga).executeAsBlocking()
    }

    /**
     * Changes the active display mode.
     */
    fun swapDisplayMode() {
        prefs.catalogueAsList().set(!isListMode)
    }

    fun setSourceFilter(selectedFilters: List<OnlineSource.Filter>) {
        this.filters = selectedFilters
        restartPager(this.query)
    }

}
