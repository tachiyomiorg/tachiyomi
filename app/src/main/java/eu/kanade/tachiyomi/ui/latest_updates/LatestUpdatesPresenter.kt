package eu.kanade.tachiyomi.ui.latest_updates

import android.os.Bundle
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.source.EN
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.online.LoginSource
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.util.NoSuchElementException

/**
 * Presenter of [LatestUpdatesFragment].
 */
class LatestUpdatesPresenter : BasePresenter<LatestUpdatesFragment>() {

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
     * Pager containing a list of manga results.
     */
    private lateinit var updatesPager: LatestUpdatesPager

    /**
     * Subject that initializes a list of manga.
     */
    private val mangaDetailSubject = PublishSubject.create<List<Manga>>()

    /**
     * Whether the view is in list mode or not.
     */
    var isListMode: Boolean = false
        private set

    /**
     * Subscription for the updatesPager.
     */
    private var pagerSubscription: Subscription? = null

    /**
     * Subscription for one request from the updatesPager.
     */
    private var pageSubscription: Subscription? = null

    /**
     * Subscription to initialize manga details.
     */
    private var initializerSubscription: Subscription? = null

    /**
     * Override val source.
     */
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        try {
            source = getLastUsedSource()
        } catch (error: NoSuchElementException) {
            return
        }

        add(prefs.catalogueAsList().asObservable()
                .subscribe { setDisplayMode(it) })

        restartPager()
    }

    /**
     * Restarts the updatesPager for the active source.
     */
    fun restartPager() {
        if (!isListMode) {
            subscribeToMangaInitializer()
        }

        // Create a new pager.
        updatesPager = LatestUpdatesPager(source)

        // Prepare the pager.
        pagerSubscription?.let { remove(it) }
        pagerSubscription = updatesPager.results()
                .subscribeReplay({ view, page ->
                    view.onAddPage(page.page, page.mangas)
                }, { view, error ->
                    Timber.e(error)
                })

        // Request first page.
        requestNext()
    }

    /**
     * Requests the next page for the active updatesPager.
     */
    fun requestNext() {
        if (!hasNextPage()) return

        pageSubscription?.let { remove(it) }
        pageSubscription = updatesPager.requestNext { getPageTransformer(it) }
                .subscribeFirst({ view, page ->
                    // Nothing to do when onNext is emitted.
                }, LatestUpdatesFragment::onAddPageError)
    }

    /**
     * Returns true if the last fetched page has a next page.
     */
    fun hasNextPage(): Boolean {
        return updatesPager.hasNextPage()
    }

    /**
     * Sets the active source and restarts the updatesPager.
     */
    fun setActiveSource(source: OnlineSource) {
        prefs.lastUsedCatalogueSource().set(source.id)
        if (source.supportsLatest)
            this.source = source
        else this.source = findFirstValidSource()

        restartPager()
    }

    /**
     * Sets the display mode.
     */
    private fun setDisplayMode(asList: Boolean) {
        isListMode = asList
        if (asList) {
            initializerSubscription?.let { remove(it) }
        } else {
            subscribeToMangaInitializer()
        }
    }

    /**
     * Subscribes to the initializer of manga details and updates the view if needed.
     */
    private fun subscribeToMangaInitializer() {
        initializerSubscription?.let { remove(it) }
        initializerSubscription = mangaDetailSubject.observeOn(Schedulers.io())
                .flatMap { Observable.from(it) }
                .filter { !it.initialized }
                .concatMap { getMangaDetailsObservable(it) }
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ manga ->
                    @Suppress("DEPRECATION")
                    view?.onMangaInitialized(manga)
                }, { error ->
                    Timber.e(error)
                })
                .apply { add(this) }
    }

    /**
     * Returns the function to apply to the observable of the list of manga from the source.
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
     */
    fun initializeMangas(mangas: List<Manga>) {
        mangaDetailSubject.onNext(mangas)
    }

    /**
     * Returns an observable of manga that initializes the given manga.
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
     */
    fun getLastUsedSource(): OnlineSource {
        val id = prefs.lastUsedCatalogueSource().get() ?: -1
        val source = sourceManager.get(id)
        if (isValidSource(source as OnlineSource?) != 2) {
            return findFirstValidSource()
        }
        return source as OnlineSource
    }

    /**
     * Checks if the given source is valid.
     */
    fun isValidSource(source: OnlineSource?): Int {
        if (source == null) return 0

        if (source.supportsLatest) {
            if (source is LoginSource) {
                if (source.isLogged() || (prefs.sourceUsername(source)
                        != "" && prefs.sourcePassword(source) != ""))
                    return 2 else return 1
            }
            return 2
        }
        return 0
    }

    /**
     * Finds the first valid source.
     */
    fun findFirstValidSource(): OnlineSource {
        return sources.first { isValidSource(it) == 2 }
    }

    /**
     * Returns a list of enabled sources ordered by language and name.
     */
    private fun getEnabledSources(): List<OnlineSource> {
        val languages = prefs.enabledLanguages().getOrDefault()
        val hiddenCatalogues = prefs.hiddenCatalogues().getOrDefault()

        // Ensure at least one language
        if (languages.isEmpty()) {
            languages.add(EN.code)
        }

        return sourceManager.getOnlineSources()
                .filter { it.lang.code in languages }
                .filterNot { it.id.toString() in hiddenCatalogues }
                .sortedBy { "(${it.lang.code}) ${it.name}" }
    }

    /**
     * Adds or removes a manga from the library.
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

}
