package eu.kanade.tachiyomi.ui.manga.track

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
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

    private val trackManager: TrackManager by injectLazy()

    lateinit var manga: Manga
        private set

    private var trackList: List<TrackItem> = emptyList()

    private val loggedServices by lazy { trackManager.services.filter { it.isLogged } }

    var selectedService: TrackService? = null

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
        trackSubscription = db.getTracks(manga)
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
                .filter { it.track != null }
                .concatMap { item ->
                    item.service.refresh(item.track!!)
                            .flatMap { db.insertTrack(it).asRxObservable() }
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

    fun registerTracking(item: Track?) {
        val service = selectedService ?: return

        if (item != null) {
            item.manga_id = manga.id!!
            add(service.bind(item)
                    .flatMap { db.insertTrack(item).asRxObservable() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ },
                            { error -> context.toast(error.message) }))
        } else {
            db.deleteTrackForManga(manga, service).executeAsBlocking()
        }
    }

    private fun updateRemote(track: Track, service: TrackService) {
        service.update(track)
                .flatMap { db.insertTrack(track).asRxObservable() }
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
        val track = item.track!!
        track.status = item.service.getStatusList()[index]
        updateRemote(track, item.service)
    }

    fun setScore(item: TrackItem, score: Int) {
        val track = item.track!!
        track.score = score.toFloat()
        updateRemote(track, item.service)
    }

    fun setLastChapterRead(item: TrackItem, chapterNumber: Int) {
        val track = item.track!!
        track.last_chapter_read = chapterNumber
        updateRemote(track, item.service)
    }

}