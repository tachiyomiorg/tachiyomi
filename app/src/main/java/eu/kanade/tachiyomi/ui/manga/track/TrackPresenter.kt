package eu.kanade.tachiyomi.ui.manga.track

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.system.toast
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackPresenter(
    val manga: Manga,
    preferences: PreferencesHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get()
) : BasePresenter<TrackController>() {

    private val context = preferences.context

    private var trackList: List<TrackItem> = emptyList()

    private val loggedServices by lazy { trackManager.services.filter { it.isLogged } }

    private var trackSubscription: Subscription? = null

    private var searchSubscription: Subscription? = null

    private var refreshSubscription: Subscription? = null

    private var chapters = emptyList<Chapter>()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        chapters = db.getChapters(manga).executeAsBlocking()

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
            .subscribeLatestCache(TrackController::onNextTrackings)
    }

    fun refresh() {
        refreshSubscription?.let { remove(it) }
        refreshSubscription = Observable.from(trackList)
            .filter { it.track != null }
            .flatMap { item ->
                item.service.refresh(item.track!!)
                    .flatMap { db.insertTrack(it).asRxObservable() }
                    .map { item }
                    .onErrorReturn { item }
            }
            .toList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ -> view.onRefreshDone() },
                TrackController::onRefreshError
            )
    }

    fun search(query: String, service: TrackService) {
        searchSubscription?.let { remove(it) }
        searchSubscription = service.search(query)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                TrackController::onSearchResults,
                TrackController::onSearchResultsError
            )
    }

    fun registerTracking(item: Track?, service: TrackService) {
        if (item != null) {
            item.manga_id = manga.id!!
            add(
                service.bind(item)
                    .flatMap { db.insertTrack(item).asRxObservable() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { },
                        { error -> context.toast(error.message) }
                    )
            )
        } else {
            unregisterTracking(service)
        }
    }

    fun unregisterTracking(service: TrackService) {
        db.deleteTrackForManga(manga, service).executeAsBlocking()
    }

    private fun updateRemote(track: Track, service: TrackService) {
        service.update(track)
            .flatMap { db.insertTrack(track).asRxObservable() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ -> view.onRefreshDone() },
                { view, error ->
                    view.onRefreshError(error)

                    // Restart on error to set old values
                    fetchTrackings()
                }
            )
    }

    fun setStatus(item: TrackItem, index: Int) {
        val track = item.track!!
        track.status = item.service.getStatusList()[index]
        if (track.status == item.service.getCompletionStatus() && track.total_chapters != 0) {
            track.last_chapter_read = track.total_chapters
        }
        updateRemote(track, item.service)
    }

    fun setScore(item: TrackItem, index: Int) {
        val track = item.track!!
        track.score = item.service.indexToScore(index)
        updateRemote(track, item.service)
    }

    fun setLastChapterRead(item: TrackItem, chapterNumber: Int) {
        val track = item.track!!
        track.last_chapter_read = chapterNumber
        if (track.total_chapters != 0 && track.last_chapter_read == track.total_chapters) {
            track.status = item.service.getCompletionStatus()
        }
        updateRemote(track, item.service)
    }

    fun setStartDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.started_reading_date = date
        updateRemote(track, item.service)
    }

    fun setFinishDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.finished_reading_date = date
        updateRemote(track, item.service)
    }

    fun importChapters(latestTrackedChapter: Int) {
        // sort chapters by source order such that sortedChapters[0] will return the latest source chapter
        val sortedChapters = chapters.sortedByDescending { it.source_order }
        var i: Int = 0

        // reads chapter until latestTrackedChapter or it reaches maximum chapter number
        while (i < sortedChapters.count() && sortedChapters[i].chapter_number <= latestTrackedChapter) {

            sortedChapters[i].read = true
            i++
        }
        // update database progress
        db.updateChaptersProgress(sortedChapters).executeAsBlocking()
    }
}
