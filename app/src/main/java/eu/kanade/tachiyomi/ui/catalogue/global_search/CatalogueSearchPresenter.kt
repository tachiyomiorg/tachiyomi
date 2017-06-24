package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.os.Bundle
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
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

class CatalogueSearchPresenter(
        val sourceManager: SourceManager = Injekt.get(),
        val db: DatabaseHelper = Injekt.get(),
        val prefs: PreferencesHelper = Injekt.get(),
        val coverCache: CoverCache = Injekt.get()
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
    private var pagerSubscription: Subscription? = null

    /**
     * Subscription to initialize manga details.
     */
    private var initializerSubscription: Subscription? = null

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (savedState != null) {
            query = savedState.getString(CataloguePresenter::query.name, "")
        }
    }

    fun globalSource(query: String) {
        subscribeToMangaInitializer()
        this.query = query

        // Prepare the pager.
        pagerSubscription?.let { remove(it) }
        pagerSubscription = Observable.from(sources)
                .observeOn(Schedulers.io())
                .flatMap {
                    val source = it
                    it.fetchSearchManga(1, query, FilterList())
                            .onExceptionResumeNext(Observable.empty())
                            .flatMap { Observable.from(it.mangas) }
                            .take(10)
                            .map { networkToLocalManga(it, source.id) }
                            .toList()
                            .doOnNext { initializeMangas(it, source) }
                            .map { Pair(it, source) }
                            .map(::CatalogueSearchItem)

                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeReplay({ view, manga ->
                    view.updateList(manga)
                }, { _, error ->
                    Timber.e(error)
                })
    }

    override fun onSave(state: Bundle) {
        state.putString(CataloguePresenter::query.name, query)
        super.onSave(state)
    }




    /**
     * Returns a list of enabled sources ordered by language and name.
     */
    private fun getEnabledSources(): List<CatalogueSource> {
        val languages = prefs.enabledLanguages().getOrDefault()
        val hiddenCatalogues = prefs.hiddenCatalogues().getOrDefault()

        // Ensure at least one language
        if (languages.isEmpty()) {
            languages.add("en")
        }

        return sourceManager.getCatalogueSources()
                .filter { it.lang in languages }
                .filterNot { it.id.toString() in hiddenCatalogues }
                .sortedBy { "(${it.lang}) ${it.name}" }
    }

    /**
     * Initialize a list of manga.
     *
     * @param mangas the list of manga to initialize.
     */
    fun initializeMangas(mangas: List<Manga>, source: Source) {
        mangaDetailSubject.onNext(Pair(mangas, source))
    }

    /**
     * Subscribes to the initializer of manga details and updates the view if needed.
     */
    private fun subscribeToMangaInitializer() {
        initializerSubscription?.let { remove(it) }
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
                .apply { add(this) }
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

    fun getSource(key: Long): Source? {
        return sourceManager.get(key)
    }

}