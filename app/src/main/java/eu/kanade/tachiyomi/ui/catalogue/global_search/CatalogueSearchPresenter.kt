package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.catalogue.CataloguePresenter
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [CatalogueSearchController]
 * Function calls should be done from here. UI calls should be done from the controller.
 *
 * @param sourceManager manages the different sources.
 * @param preferencesHelper manages the database calls.
 */
class CatalogueSearchPresenter(
        val sourceManager: SourceManager = Injekt.get(),
        val db: DatabaseHelper = Injekt.get(),
        val preferencesHelper: PreferencesHelper = Injekt.get()
) : BasePresenter<CatalogueSearchController>() {

    /**
     * Enabled sources.
     */
    val sources by lazy { getEnabledSources() }

    /**
     * Query from the view.
     */
    var query = ""
        private set

    /**
     * Subject that initializes a list of manga.
     */
    private val mangaDetailSubject = PublishSubject.create<Pair<List<Manga>, Source>>()

    /**
     * Subscription for the pager.
     */
    private var searchSubscription: Subscription? = null

    /**
     * Subscription to initialize manga details.
     */
    private var initializerSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (savedState != null) {
            query = savedState.getString(CataloguePresenter::query.name, "")
        }
    }

    override fun onDestroy() {
        initializerSubscription?.unsubscribe()
        searchSubscription?.unsubscribe()
        super.onDestroy()
    }

    override fun onSave(state: Bundle) {
        state.putString(CataloguePresenter::query.name, query)
        super.onSave(state)
    }

    /**
     * Returns a list of enabled sources ordered by language and name.
     *
     * @return list containing enabled sources.
     */
    fun getEnabledSources(): List<CatalogueSource> {
        val languages = preferencesHelper.enabledLanguages().getOrDefault()
        val hiddenCatalogues = preferencesHelper.hiddenCatalogues().getOrDefault()

        return sourceManager.getCatalogueSources()
                .filter { it.lang in languages }
                .filterNot { it is LoginSource && !it.isLogged() }
                .filterNot { it.id.toString() in hiddenCatalogues }
                .sortedBy { "(${it.lang}) ${it.name}" } +
                sourceManager.get(LocalSource.ID) as LocalSource
    }

    fun startGlobalSearch(query: String) {
        subscribeToMangaInitializer()

        searchSubscription?.unsubscribe()
        searchSubscription = Observable.from(sources)
                .observeOn(Schedulers.io())
                .flatMap {
                    val source = it
                    it.fetchSearchManga(1, query, FilterList())
                            .onExceptionResumeNext(Observable.empty()) // Ignore timeouts.
                            .flatMap { Observable.from(it.mangas).take(10) } // Get at most 10 manga from search result.
                            .map { networkToLocalManga(it, source.id) } // Convert to local manga.
                            .toList()
                            .doOnNext { initializeManga(it, source) } // Load manga covers.
                            .map { Pair(it, source) } // Create pair
                            .map(::CatalogueSearchItem) // Map to CatalogueItem.
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeReplay({ view, manga ->
                    view.addSearchResult(manga)
                }, { _, error ->
                    Timber.e(error)
                })
    }
    /**
     * Initialize a list of manga.
     *
     * @param manga the list of manga to initialize.
     */
    fun initializeManga(manga: List<Manga>, source: Source) {
        mangaDetailSubject.onNext(Pair(manga, source))
    }

    /**
     * Subscribes to the initializer of manga details and updates the view if needed.
     */
    private fun subscribeToMangaInitializer() {
        initializerSubscription?.unsubscribe()
        initializerSubscription = mangaDetailSubject.observeOn(Schedulers.io())
                .flatMap {
                    val source = it.second
                    Observable.from(it.first).filter { it.thumbnail_url == null && !it.initialized }
                            .map { Pair(it, source) }

                }
                .concatMap { getMangaDetailsObservable(it.first, it.second) }
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ manga ->
                    @Suppress("DEPRECATION")
                    view?.onMangaInitialized(manga)
                }, { error ->
                    Timber.e(error)
                })
    }

    /**
     * Returns an observable of manga that initializes the given manga.
     *
     * @param manga the manga to initialize.
     * @return an observable of the manga to initialize
     */
    private fun getMangaDetailsObservable(manga: Manga, source: Source): Observable<Manga> {
        return source.fetchMangaDetails(manga)
                .flatMap { networkManga ->
                    manga.copyFrom(networkManga)
                    manga.initialized = true
                    db.insertManga(manga).executeAsBlocking()
                    Observable.just(manga)
                }
                .onErrorResumeNext { Observable.just(manga) }
    }

    /**
     * Returns a manga from the database for the given manga from network. It creates a new entry
     * if the manga is not yet in the database.
     *
     * @param sManga the manga from the source.
     * @return a manga from the database.
     */
    private fun networkToLocalManga(sManga: SManga, sourceId: Long): Manga {
        var localManga = db.getManga(sManga.url, sourceId).executeAsBlocking()
        if (localManga == null) {
            val newManga = Manga.create(sManga.url, sManga.title, sourceId)
            newManga.copyFrom(sManga)
            val result = db.insertManga(newManga).executeAsBlocking()
            newManga.id = result.insertedId()
            localManga = newManga
        }
        return localManga
    }
}