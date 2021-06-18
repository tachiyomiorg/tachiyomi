package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.toMangaInfo
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.toSManga
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourcePresenter
import eu.kanade.tachiyomi.util.lang.runAsObservable
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of [GlobalSearchController]
 * Function calls should be done from here. UI calls should be done from the controller.
 *
 * @param sourceManager manages the different sources.
 * @param db manages the database calls.
 * @param preferences manages the preference calls.
 */
open class GlobalSearchPresenter(
    val initialQuery: String? = "",
    val initialExtensionFilter: String? = null,
    val sourceManager: SourceManager = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<GlobalSearchController>() {

    /**
     * Enabled sources.
     */
    val sources by lazy { getSourcesToQuery() }

    /**
     * Fetches the different sources by user settings.
     */
    private var fetchSourcesSubscription: Subscription? = null

    /**
     * Subject which fetches image of given manga.
     */
    private val fetchImageSubject = PublishSubject.create<Pair<List<Manga>, Source>>()

    /**
     * Subscription for fetching images of manga.
     */
    private var fetchImageSubscription: Subscription? = null

    private val extensionManager: ExtensionManager by injectLazy()

    private var extensionFilter: String? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        extensionFilter = savedState?.getString(GlobalSearchPresenter::extensionFilter.name)
            ?: initialExtensionFilter

        // Perform a search with previous or initial state
        search(
            savedState?.getString(BrowseSourcePresenter::query.name)
                ?: initialQuery.orEmpty()
        )
    }

    override fun onDestroy() {
        fetchSourcesSubscription?.unsubscribe()
        fetchImageSubscription?.unsubscribe()
        super.onDestroy()
    }

    override fun onSave(state: Bundle) {
        state.putString(BrowseSourcePresenter::query.name, query)
        state.putString(GlobalSearchPresenter::extensionFilter.name, extensionFilter)
        super.onSave(state)
    }

    /**
     * Returns a list of enabled sources ordered by language and name, with pinned catalogues
     * prioritized.
     *
     * @return list containing enabled sources.
     */
    protected open fun getEnabledSources(): List<CatalogueSource> {
        val languages = preferences.enabledLanguages().get()
        val disabledSourceIds = preferences.disabledSources().get()
        val pinnedSourceIds = preferences.pinnedSources().get()

        return sourceManager.getCatalogueSources()
            .filter { it.lang in languages }
            .filterNot { it.id.toString() in disabledSourceIds }
            .sortedWith(compareBy({ it.id.toString() !in pinnedSourceIds }, { "${it.name.lowercase()} (${it.lang})" }))
    }

    private fun getSourcesToQuery(): List<CatalogueSource> {
        val filter = extensionFilter
        val enabledSources = getEnabledSources()
        var filteredSources: List<CatalogueSource>? = null

        if (!filter.isNullOrEmpty()) {
            filteredSources = extensionManager.installedExtensions
                .filter { it.pkgName == filter }
                .flatMap { it.sources }
                .filter { it in enabledSources }
                .filterIsInstance<CatalogueSource>()
        }

        if (filteredSources != null && filteredSources.isNotEmpty()) {
            return filteredSources
        }

        val onlyPinnedSources = preferences.searchPinnedSourcesOnly()
        val pinnedSourceIds = preferences.pinnedSources().get()

        return enabledSources
            .filter { if (onlyPinnedSources) it.id.toString() in pinnedSourceIds else true }
    }

    /**
     * Creates a catalogue search item
     */
    protected open fun createCatalogueSearchItem(source: CatalogueSource, results: List<GlobalSearchCardItem>?): GlobalSearchItem {
        return GlobalSearchItem(source, results)
    }

    /**
     * Initiates a search for manga per catalogue.
     *
     * @param query query on which to search.
     */
    fun search(query: String) {
        // Return if there's nothing to do
        if (this.query == query) return

        // Update query
        this.query = query

        // Create image fetch subscription
        initializeFetchImageSubscription()

        // Create items with the initial state
        val initialItems = sources.map { createCatalogueSearchItem(it, null) }
        var items = initialItems

        val pinnedSourceIds = preferences.pinnedSources().get()

        fetchSourcesSubscription?.unsubscribe()
        fetchSourcesSubscription = Observable.from(sources)
            .flatMap(
                { source ->
                    Observable.defer { source.fetchSearchManga(1, query, FilterList()) }
                        .subscribeOn(Schedulers.io())
                        .onErrorReturn { MangasPage(emptyList(), false) } // Ignore timeouts or other exceptions
                        .map { it.mangas }
                        .map { list -> list.map { networkToLocalManga(it, source.id) } } // Convert to local manga
                        .doOnNext { fetchImage(it, source) } // Load manga covers
                        .map { list -> createCatalogueSearchItem(source, list.map { GlobalSearchCardItem(it) }) }
                },
                5
            )
            .observeOn(AndroidSchedulers.mainThread())
            // Update matching source with the obtained results
            .map { result ->
                items
                    .map { item -> if (item.source == result.source) result else item }
                    .sortedWith(
                        compareBy(
                            // Bubble up sources that actually have results
                            { it.results.isNullOrEmpty() },
                            // Same as initial sort, i.e. pinned first then alphabetically
                            { it.source.id.toString() !in pinnedSourceIds },
                            { "${it.source.name.lowercase()} (${it.source.lang})" }
                        )
                    )
            }
            // Update current state
            .doOnNext { items = it }
            // Deliver initial state
            .startWith(initialItems)
            .subscribeLatestCache(
                { view, manga ->
                    view.setItems(manga)
                },
                { _, error ->
                    Timber.e(error)
                }
            )
    }

    /**
     * Initialize a list of manga.
     *
     * @param manga the list of manga to initialize.
     */
    private fun fetchImage(manga: List<Manga>, source: Source) {
        fetchImageSubject.onNext(Pair(manga, source))
    }

    /**
     * Subscribes to the initializer of manga details and updates the view if needed.
     */
    private fun initializeFetchImageSubscription() {
        fetchImageSubscription?.unsubscribe()
        fetchImageSubscription = fetchImageSubject.observeOn(Schedulers.io())
            .flatMap { (first, source) ->
                Observable.from(first)
                    .filter { it.thumbnail_url == null && !it.initialized }
                    .map { Pair(it, source) }
                    .concatMap { runAsObservable({ getMangaDetails(it.first, it.second) }) }
                    .map { Pair(source as CatalogueSource, it) }
            }
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { (source, manga) ->
                    @Suppress("DEPRECATION")
                    view?.onMangaInitialized(source, manga)
                },
                { error ->
                    Timber.e(error)
                }
            )
    }

    /**
     * Initializes the given manga.
     *
     * @param manga the manga to initialize.
     * @return The initialized manga.
     */
    private suspend fun getMangaDetails(manga: Manga, source: Source): Manga {
        val networkManga = source.getMangaDetails(manga.toMangaInfo())
        manga.copyFrom(networkManga.toSManga())
        manga.initialized = true
        db.insertManga(manga).executeAsBlocking()
        return manga
    }

    /**
     * Returns a manga from the database for the given manga from network. It creates a new entry
     * if the manga is not yet in the database.
     *
     * @param sManga the manga from the source.
     * @return a manga from the database.
     */
    protected open fun networkToLocalManga(sManga: SManga, sourceId: Long): Manga {
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
