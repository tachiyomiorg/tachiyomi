package eu.kanade.tachiyomi.ui.manga.info

import android.os.Bundle
import com.pushtorefresh.storio.sqlite.operations.put.PutResults
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.data.mangasync.MangaSyncService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.manga.MangaEvent
import eu.kanade.tachiyomi.util.SharedData
import org.jetbrains.annotations.Nullable
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

/**
 * Presenter that handles Observable calls.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 * UI related actions should be called from [MangaInfoFragment].
 */
class MangaInfoPresenter : BasePresenter<MangaInfoFragment>() {
    companion object {
        /**
         * The id of the restartable.
         */
        const private val GET_MANGA = 1

        /**
         * The id of the restartable.
         */
        const private val FETCH_MANGA_INFO = 2

        /**
         * The id of the restartable.
         */
        const private val GET_MANGA_SYNC = 3

        /**
         * The id of the restartable.
         */
        const private val FETCH_MANGA_SYNC = 4

        /**
         * Prefix for RealQuery results
         */
        const private val PREFIX_MY = "my:"

    }

    /**
     * Provides operations to manage the database through its interfaces.
     */
    val databaseHelper: DatabaseHelper by injectLazy()

    /**
     * Provides operations to manage [Source] objects through its interfaces
     */
    val sourceManager: SourceManager by injectLazy()

    /**
     * Provides operations to manage preferences
     */
    val preferenceHelper: PreferencesHelper by injectLazy()

    /**
     * Provides operations to manga image cache.
     */
    val coverCache: CoverCache by injectLazy()

    /**
     * Provides operations to manage [MangaSync] objects through its interfaces
     */
    val syncManager: MangaSyncManager by injectLazy()

    /**
     * Provides the attributes of a [Manga] object
     */
    lateinit var manga: Manga
        private set

    /**
     * Provides the attributes of a [Source] object
     */
    lateinit var source: Source
        private set

    /**
     * String containing search query
     */
    private var query: String? = null

    /**
     * List containing bind [MangaSync] objects
     */
    private var mangaSyncList: List<MangaSync> = emptyList()

    val statusMap by lazy {
        getDefaultService().statusMap
    }

    /**
     * Called when the activity is starting.
     * @param savedState Contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    override fun onCreate(@Nullable savedState: Bundle?) {
        super.onCreate(savedState)

        // Notify the view a manga is available or has changed.
        startableLatestCache(GET_MANGA,
                { Observable.just(manga) },
                { view, manga -> view.onNextManga(manga, source) })

        // Fetch manga info from source.
        startableFirst(FETCH_MANGA_INFO,
                { fetchMangaObs() },
                { view, manga -> view.onFetchMangaDone() },
                { view, error -> view.onFetchMangaError(error) })

        startableFirst(FETCH_MANGA_SYNC,
                { getRemoteMangaSyncObservable() },
                { view, result -> view.onRefreshSyncDone() },
                { view, error -> view.onFetchMangaError(error) })

        // Fetch bound MangaSync objects from database
        startableLatestCache(GET_MANGA_SYNC,
                {
                    databaseHelper.getMangasSync(manga).asRxObservable()
                            .doOnNext { mangaSyncList = it }
                            .observeOn(AndroidSchedulers.mainThread())
                },
                { view, list ->
                    val syncList = getUnbindMangaSync(list.toMutableList())
                    view.onNextMangaSync(syncList)
                },
                { view, error -> error.message })

        // Retrieve selected manga
        manga = SharedData.get(MangaEvent::class.java)?.manga ?: return
        // Get source of manga
        source = sourceManager.get(manga.source)!!

        // Notify View of manga change
        start(GET_MANGA)

        // Collect MangaSync data
        if (manga.favorite)
            start(GET_MANGA_SYNC)

        // Update chapter count
        SharedData.get(ChapterCountEvent::class.java)?.let {
            it.observable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeLatestCache({ view, count -> view.setChapterCount(count) })
        }
    }

    /**
     * Returns list with missing [MangaSync] objects.
     * Note: Mangasync is only added when service is logged
     * @param mangaSyncList list containing [MangaSync] objects.
     * @return list containing logged [MangaSync] objects
     */
    fun getUnbindMangaSync(mangaSyncList: MutableList<MangaSync>): List<MangaSync> {
        val services = syncManager.services.filter { it.isLogged }
        if (services.size != mangaSyncList.size) {
            services.forEach { syncService ->
                        if (mangaSyncList.find { it.sync_id == syncService.id } == null)
                            mangaSyncList.add(MangaSync.create(syncService.id))
                    }
        }
        mangaSyncList.sortBy { it.sync_id }
        return mangaSyncList
    }

    /**
     * Returns an [Observable] containing the [Manga] object using the source
     * Note: ObserveOn already initialized
     * @return Observable containing Manga object.
     */
    private fun fetchMangaObs(): Observable<Manga> {
        return source.fetchMangaDetails(manga)
                .flatMap { networkManga ->
                    manga.copyFrom(networkManga)
                    databaseHelper.insertManga(manga).executeAsBlocking()
                    Observable.just<Manga>(manga)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { start(GET_MANGA) }
    }

    /**
     * Returns an [Observable] containing a list with [MangaSync] objects using the search query
     * Note: ObserveOn already initialized
     * @param syncId id of the [MangaSync] service
     * @return Observable containing list with [MangaSync] objects.
     */
    fun getSearchResultsObservable(syncId: Int): Observable<List<MangaSync>> {
        return query?.let { query ->
            val observable: Observable<List<MangaSync>>
            if (query.startsWith(PREFIX_MY)) {
                val realQuery = query.substring(PREFIX_MY.length).toLowerCase().trim()
                observable = syncManager.getService(syncId).getList()
                        .flatMap { Observable.from(it) }
                        .filter { it.title.toLowerCase().contains(realQuery) }
                        .toList()
            } else {
                observable = syncManager.getService(syncId).search(query)
            }
            observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        } ?: Observable.error(Exception("Null query"))
    }

    /**
     * Called to start fetching manga info from source
     * [FETCH_MANGA_INFO] will only be called if there is no active subscription.
     */
    fun fetchMangaFromSource() {
        if (isUnsubscribed(FETCH_MANGA_INFO)) {
            start(FETCH_MANGA_INFO)
        }
    }

    /**
     * Called to start fetching manga sync info from service
     * [FETCH_MANGA_SYNC] will only be called if there is no active subscription.
     */
    fun fetchSyncInfoFromSource() {
        if (isUnsubscribed(FETCH_MANGA_SYNC)) {
            start(FETCH_MANGA_SYNC)
        }
    }

    /**
     * Called to check if [MangaSync] object is bind to the default Manga service
     * @return result of check if bind to default manga service
     */
    fun isDefaultService(mangaSync: MangaSync): Boolean {
        return mangaSync.is_bind && mangaSync.sync_id == preferenceHelper.defaultServiceId().getOrDefault()
    }

    /**
     * Called to remove or add manga to library.
     * Removes cover cache if removed.
     */
    fun addOrRemoveFromLibrary() {
        manga.favorite = !manga.favorite
        // Remove cover cache if removed from library
        if (!manga.favorite) {
            coverCache.deleteFromCache(manga.thumbnail_url)
            // Remove bind
            if (mangaSyncList.isNotEmpty())
                mangaSyncList.forEach { unbindMangaSync(it) }
        } else {
            start(GET_MANGA_SYNC)
        }
        databaseHelper.insertManga(manga).asRxObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { start(GET_MANGA) }
    }

    /**
     * Called to check if [Manga] object has bind [MangaSync]
     * @return result of check if manga has bind manga sync objects
     */
    fun hasBind(): Boolean {
        mangaSyncList.forEach { if (it.is_bind) return true }
        return false
    }

    /**
     * Called to bind [Manga] to correct service
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     */
    fun registerManga(mangaSync: MangaSync) {
        mangaSync.manga_id = manga.id!!
        add(
                syncManager.getService(mangaSync.sync_id).bind(mangaSync)
                        .flatMap { databaseHelper.insertMangaSync(mangaSync).asRxObservable() }
                        .doOnCompleted { start(GET_MANGA_SYNC) }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { }
        )
    }

    /**
     * Called to remove [MangaSync] object from database
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     */
    fun unbindMangaSync(mangaSync: MangaSync) {
        databaseHelper.deleteMangaSync(mangaSync).asRxObservable()
                .doOnCompleted { start(GET_MANGA_SYNC) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { }
    }

    /**
     * Returns an [Observable] containing a list with [MangaSync] objects.
     * Note: ObserveOn already initialized
     * @return Observable containing list with updated [MangaSync] objects.
     */
    fun getRemoteMangaSyncObservable(): Observable<PutResults<MangaSync>>? {
        return Observable.from(mangaSyncList).flatMap { mangaSync ->
            syncManager.getService(mangaSync.sync_id).getList()
                    .flatMap { Observable.from(it) }
                    .filter { it.remote_id == mangaSync.remote_id }
                    .map {
                        mangaSync.copyPersonalFrom(it)
                        mangaSync.total_chapters = it.total_chapters
                        mangaSync
                    }
        }.toList().subscribeOn(Schedulers.io()).flatMap { databaseHelper.insertMangasSync(it).asRxObservable() }
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Called to fetch search result from service API
     * @param query search query
     * @param syncId Id of sync service
     * @param force ignore duplicate query's
     */
    fun searchManga(query: String?, syncId: Int, force: Boolean = false) {
        if (query.isNullOrEmpty() || (query == this.query && !force))
            return

        this.query = query

        // Fetch search results from Service API
        // Used to get a list of recently read manga
        getSearchResultsObservable(syncId)
                .subscribeLatestCache({ view, results ->
                    view.setSearchResults(results)
                }, { view, error -> view.setSearchResultsError(error) })
    }

    fun getDefaultService(): MangaSyncService {
        return syncManager.getService(preferenceHelper.defaultServiceId().getOrDefault())
    }

    /**
     * Returns the spinner position of a status
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     * @return spinner position
     */
    fun getStatus(mangaSync: MangaSync): String {
        return syncManager.getService(mangaSync.sync_id).getStatus(mangaSync.status)
    }

    /**
     * Called to update status of bind manga sync
     * @param position selected spinner position
     */
    fun setStatus(status: String) {
        mangaSyncList.forEach {
            it.status = syncManager.getService(it.sync_id).getStatus(status)
        }
        updateRemote(mangaSyncList)
    }

    /**
     * Called to update score of bind manga sync
     * @param score score given by user
     */
    fun setScore(score: Int) {
        mangaSyncList.forEach {
            it.score = syncManager.getService(it.sync_id).getServiceUserScore(score)
        }
        updateRemote(mangaSyncList)
    }

    /**
     * Returns the score given by user
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     * @return score given by user
     */
    fun getScore(mangaSync: MangaSync): Float {
        return syncManager.getService(mangaSync.sync_id).getUserScore(mangaSync)
    }

    /**
     * Returns the average score of manga
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     * @return average score of manga (from service)
     */
    fun getRemoteScore(mangaSync: MangaSync): Float {
        return syncManager.getService(mangaSync.sync_id).getRemoteScore(mangaSync)
    }

    /**
     * Called to update last read page of bind manga sync
     * @param chapterNumber number of last read page
     */
    fun setLastChapterRead(chapterNumber: Int) {
        var status = 0
        mangaSyncList.forEach {
            it.last_chapter_read = chapterNumber
            if (isDefaultService(it)) {
                status = it.status
            }
        }

        if (preferenceHelper.autoUpdateStatusMangaSync()) {
            if (chapterNumber == 0) {
                if (status != getDefaultService().getStatus(context.getString(R.string.on_hold)))
                    updateRemote(mangaSyncList,
                            { setStatus(context.getString(R.string.on_hold)) })
                else updateRemote(mangaSyncList)
            } else {
                if (status != getDefaultService().getStatus(context.getString(R.string.reading)))
                    updateRemote(mangaSyncList, { setStatus(context.getString(R.string.reading)) })
                else updateRemote(mangaSyncList)
            }
        }
    }

    /**
     * Called to update the remote service
     * @param mangaSyncList list containing [MangaSync] objects.
     */
    private fun updateRemote(mangaSyncList: List<MangaSync>, func: () -> Unit = {}) {
        add(Observable.from(mangaSyncList).flatMap { mangaSync ->
            syncManager.getService(mangaSync.sync_id).update(mangaSync)
        }.toList()
                .subscribeOn(Schedulers.io())
                .flatMap { databaseHelper.insertMangasSync(it).asRxObservable() }
                .doOnCompleted { func() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({},
                        { error ->
                            Timber.e(error, error.message)
                            // Restart on error to set old values
                            start(GET_MANGA_SYNC)
                        }))
    }

    /**
     * Returns the logged status of accounts
     * @return true if one or more accounts are logged in by user.
     */
    fun isLogged(): Boolean {
        syncManager.services.forEach {
            if (it.isLogged)
                return true
        }
        return false
    }
}