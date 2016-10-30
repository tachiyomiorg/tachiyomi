package eu.kanade.tachiyomi.ui.manga.track

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.data.mangasync.MangaSyncService
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.manga.MangaEvent
import eu.kanade.tachiyomi.util.SharedData
import eu.kanade.tachiyomi.util.toast
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy

class TrackPresenter : BasePresenter<TrackFragment>() {

    private val db: DatabaseHelper by injectLazy()

    private val syncManager: MangaSyncManager by injectLazy()

    lateinit var manga: Manga
        private set

    private var trackList: List<TrackItem> = emptyList()

    private val loggedServices by lazy { syncManager.services.filter { it.isLogged } }

    var selectedService: MangaSyncService? = null

    private var trackSubscription: Subscription? = null

    private var searchSubscription: Subscription? = null

    private var refreshSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        manga = SharedData.get(MangaEvent::class.java)?.manga ?: return
        fetchTrackings()
    }

    fun fetchTrackings() {
        trackSubscription?.let { remove(it) }
        trackSubscription = db.getMangasSync(manga)
                .asRxObservable()
                .map { tracks ->
                    loggedServices.map { service ->
                        TrackItem(tracks.find { it.sync_id == service.id }, service)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { trackList = it }
                .subscribeLatestCache(TrackFragment::onNextTrackings)
    }

    fun refresh() {
        refreshSubscription?.let { remove(it) }
        refreshSubscription = Observable.from(trackList)
                .filter { it.sync != null }
                .concatMap { item ->
                    item.service.refresh(item.sync!!)
                            .flatMap { db.insertMangaSync(it).asRxObservable() }
                            .map { item }
                            .onErrorReturn { item }
                }
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst({ view, result -> view.onRefreshDone() },
                        TrackFragment::onRefreshError)
    }

    fun search(query: String) {
        val service = selectedService ?: return

        searchSubscription?.let { remove(it) }
        searchSubscription = service.search(query)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(TrackFragment::onSearchResults,
                        TrackFragment::onSearchResultsError)
    }

    fun registerTracking(item: MangaSync?) {
        val service = selectedService ?: return

        if (item != null) {
            item.manga_id = manga.id!!
            add(service.bind(item)
                    .flatMap { db.insertMangaSync(item).asRxObservable() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ },
                            { error -> context.toast(error.message) }))
        } else {
            db.deleteMangaSyncForManga(manga, service).executeAsBlocking()
        }
    }

    private fun updateRemote(sync: MangaSync, service: MangaSyncService) {
        service.update(sync)
                .flatMap { db.insertMangaSync(sync).asRxObservable() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst({ view, result -> view.onRefreshDone() },
                        { view, error ->
                            view.onRefreshError(error)

                            // Restart on error to set old values
                            fetchTrackings()
                        })
    }

    fun setStatus(item: TrackItem, index: Int) {
        val sync = item.sync!!
        sync.status = item.service.getStatusList()[index]
        updateRemote(sync, item.service)
    }

    fun setScore(item: TrackItem, score: Int) {
        val sync = item.sync!!
        sync.score = score.toFloat()
        updateRemote(sync, item.service)
    }

    fun setLastChapterRead(item: TrackItem, chapterNumber: Int) {
        val sync = item.sync!!
        sync.last_chapter_read = chapterNumber
        updateRemote(sync, item.service)
    }

}