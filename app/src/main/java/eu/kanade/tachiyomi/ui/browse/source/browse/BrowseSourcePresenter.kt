package eu.kanade.tachiyomi.ui.browse.source.browse

import android.os.Bundle
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.flexibleadapter.items.ISectionable
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.browse.source.filter.CheckboxItem
import eu.kanade.tachiyomi.ui.browse.source.filter.CheckboxSectionItem
import eu.kanade.tachiyomi.ui.browse.source.filter.GroupItem
import eu.kanade.tachiyomi.ui.browse.source.filter.HeaderItem
import eu.kanade.tachiyomi.ui.browse.source.filter.SelectItem
import eu.kanade.tachiyomi.ui.browse.source.filter.SelectSectionItem
import eu.kanade.tachiyomi.ui.browse.source.filter.SeparatorItem
import eu.kanade.tachiyomi.ui.browse.source.filter.SortGroup
import eu.kanade.tachiyomi.ui.browse.source.filter.SortItem
import eu.kanade.tachiyomi.ui.browse.source.filter.TextItem
import eu.kanade.tachiyomi.ui.browse.source.filter.TextSectionItem
import eu.kanade.tachiyomi.ui.browse.source.filter.TriStateItem
import eu.kanade.tachiyomi.ui.browse.source.filter.TriStateSectionItem
import eu.kanade.tachiyomi.util.removeCovers
import java.util.Date
import kotlinx.coroutines.flow.subscribe
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [BrowseSourceController].
 */
open class BrowseSourcePresenter(
    private val sourceId: Long,
    private val searchQuery: String? = null,
    private val sourceManager: SourceManager = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val prefs: PreferencesHelper = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get()
) : BasePresenter<BrowseSourceController>() {

    /**
     * Selected source.
     */
    lateinit var source: CatalogueSource

    /**
     * Query from the view.
     */
    var query = searchQuery ?: ""
        private set

    /**
     * Modifiable list of filters.
     */
    var sourceFilters = FilterList()
        set(value) {
            field = value
            filterItems = value.toItems()
        }

    var filterItems: List<IFlexible<*>> = emptyList()

    /**
     * List of filters used by the [Pager]. If empty alongside [query], the popular query is used.
     */
    var appliedFilters = FilterList()

    /**
     * Pager containing a list of manga results.
     */
    private lateinit var pager: Pager

    /**
     * Subject that initializes a list of manga.
     */
    private val mangaDetailSubject = PublishSubject.create<List<Manga>>()

    /**
     * Subscription for the pager.
     */
    private var pagerSubscription: Subscription? = null

    /**
     * Subscription for one request from the pager.
     */
    private var pageSubscription: Subscription? = null

    /**
     * Subscription to initialize manga details.
     */
    private var initializerSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        source = sourceManager.get(sourceId) as? CatalogueSource ?: return

        sourceFilters = source.getFilterList()

        if (savedState != null) {
            query = savedState.getString(::query.name, "")
        }

        restartPager()
    }

    override fun onSave(state: Bundle) {
        state.putString(::query.name, query)
        super.onSave(state)
    }

    /**
     * Restarts the pager for the active source with the provided query and filters.
     *
     * @param query the query.
     * @param filters the current state of the filters (for search mode).
     */
    fun restartPager(query: String = this.query, filters: FilterList = this.appliedFilters) {
        this.query = query
        this.appliedFilters = filters

        subscribeToMangaInitializer()

        // Create a new pager.
        pager = createPager(query, filters)

        val sourceId = source.id

        val sourceDisplayMode = prefs.sourceDisplayMode()

        // Prepare the pager.
        pagerSubscription?.let { remove(it) }
        pagerSubscription = pager.results()
            .observeOn(Schedulers.io())
            .map { pair -> pair.first to pair.second.map { networkToLocalManga(it, sourceId) } }
            .doOnNext { initializeMangas(it.second) }
            .map { pair -> pair.first to pair.second.map { SourceItem(it, sourceDisplayMode) } }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeReplay(
                { view, (page, mangas) ->
                    view.onAddPage(page, mangas)
                },
                { _, error ->
                    Timber.e(error)
                }
            )

        // Request first page.
        requestNext()
    }

    /**
     * Requests the next page for the active pager.
     */
    fun requestNext() {
        if (!hasNextPage()) return

        pageSubscription?.let { remove(it) }
        pageSubscription = Observable.defer { pager.requestNext() }
            .subscribeFirst(
                { _, _ ->
                    // Nothing to do when onNext is emitted.
                },
                BrowseSourceController::onAddPageError
            )
    }

    /**
     * Returns true if the last fetched page has a next page.
     */
    fun hasNextPage(): Boolean {
        return pager.hasNextPage
    }

    /**
     * Subscribes to the initializer of manga details and updates the view if needed.
     */
    private fun subscribeToMangaInitializer() {
        initializerSubscription?.let { remove(it) }
        initializerSubscription = mangaDetailSubject.observeOn(Schedulers.io())
            .flatMap { Observable.from(it) }
            .filter { it.thumbnail_url == null && !it.initialized }
            .concatMap { getMangaDetailsObservable(it) }
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { manga ->
                    @Suppress("DEPRECATION")
                    view?.onMangaInitialized(manga)
                },
                { error ->
                    Timber.e(error)
                }
            )
            .apply { add(this) }
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
                manga.initialized = true
                db.insertManga(manga).executeAsBlocking()
                Observable.just(manga)
            }
            .onErrorResumeNext { Observable.just(manga) }
    }

    /**
     * Adds or removes a manga from the library.
     *
     * @param manga the manga to update.
     */
    fun changeMangaFavorite(manga: Manga) {
        manga.favorite = !manga.favorite
        manga.date_added = when (manga.favorite) {
            true -> Date().time
            false -> 0
        }

        if (!manga.favorite) {
            manga.removeCovers(coverCache)
        }

        db.insertManga(manga).executeAsBlocking()
    }

    /**
     * Refreshes the active display mode.
     */
    fun refreshDisplayMode() {
        subscribeToMangaInitializer()
    }

    /**
     * Set the filter states for the current source.
     *
     * @param filters a list of active filters.
     */
    fun setSourceFilter(filters: FilterList) {
        restartPager(filters = filters)
    }

    open fun createPager(query: String, filters: FilterList): Pager {
        return SourcePager(source, query, filters)
    }

    private fun FilterList.toItems(): List<IFlexible<*>> {
        return mapNotNull { filter ->
            when (filter) {
                is Filter.Header -> HeaderItem(filter)
                is Filter.Separator -> SeparatorItem(filter)
                is Filter.CheckBox -> CheckboxItem(filter)
                is Filter.TriState -> TriStateItem(filter)
                is Filter.Text -> TextItem(filter)
                is Filter.Select<*> -> SelectItem(filter)
                is Filter.Group<*> -> {
                    val group = GroupItem(filter)
                    val subItems = filter.state.mapNotNull {
                        when (it) {
                            is Filter.CheckBox -> CheckboxSectionItem(it)
                            is Filter.TriState -> TriStateSectionItem(it)
                            is Filter.Text -> TextSectionItem(it)
                            is Filter.Select<*> -> SelectSectionItem(it)
                            else -> null
                        } as? ISectionable<*, *>
                    }
                    subItems.forEach { it.header = group }
                    group.subItems = subItems
                    group
                }
                is Filter.Sort -> {
                    val group = SortGroup(filter)
                    val subItems = filter.values.map {
                        SortItem(it, group)
                    }
                    group.subItems = subItems
                    group
                }
            }
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    fun getCategories(): List<Category> {
        return db.getCategories().executeAsBlocking()
    }

    /**
     * Gets the category id's the manga is in, if the manga is not in a category, returns the default id.
     *
     * @param manga the manga to get categories from.
     * @return Array of category ids the manga is in, if none returns default id
     */
    fun getMangaCategoryIds(manga: Manga): Array<Int?> {
        val categories = db.getCategoriesForManga(manga).executeAsBlocking()
        return categories.mapNotNull { it.id }.toTypedArray()
    }

    /**
     * Move the given manga to categories.
     *
     * @param categories the selected categories.
     * @param manga the manga to move.
     */
    private fun moveMangaToCategories(manga: Manga, categories: List<Category>) {
        val mc = categories.filter { it.id != 0 }.map { MangaCategory.create(manga, it) }
        db.setMangaCategories(mc, listOf(manga))
    }

    /**
     * Move the given manga to the category.
     *
     * @param category the selected category.
     * @param manga the manga to move.
     */
    fun moveMangaToCategory(manga: Manga, category: Category?) {
        moveMangaToCategories(manga, listOfNotNull(category))
    }

    /**
     * Update manga to use selected categories.
     *
     * @param manga needed to change
     * @param selectedCategories selected categories
     */
    fun updateMangaCategories(manga: Manga, selectedCategories: List<Category>) {
        if (!manga.favorite) {
            changeMangaFavorite(manga)
        }

        moveMangaToCategories(manga, selectedCategories)
    }
}
