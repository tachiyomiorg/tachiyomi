package eu.kanade.tachiyomi.ui.manga.myanimelist

import android.os.Bundle
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.event.MangaEvent
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.toast
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class MyAnimeListPresenter : BasePresenter<MyAnimeListFragment>() {

    @Inject lateinit var db: DatabaseHelper
    @Inject lateinit var syncManager: MangaSyncManager

    val myAnimeList by lazy { syncManager.myAnimeList }

    lateinit var manga: Manga
        private set

    var mangaSync: MangaSync? = null
        private set

    private var query: String? = null

    private val GET_MANGA_SYNC = 1
    private val GET_SEARCH_RESULTS = 2
    private val REFRESH = 3

    private val PREFIX_MY = "my:"

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        startableLatestCache(GET_MANGA_SYNC,
                { db.getMangaSync(manga, myAnimeList).asRxObservable()
                        .doOnNext { mangaSync = it }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()) },
                { view, mangaSync -> view.setMangaSync(mangaSync) })

        startableLatestCache(GET_SEARCH_RESULTS,
                { getSearchResultsObservable() },
                { view, results -> view.setSearchResults(results) },
                { view, error -> view.setSearchResultsError(error) })

        startableFirst(REFRESH,
                { getRefreshObservable() },
                { view, result -> view.onRefreshDone() },
                { view, error -> view.onRefreshError() })

        registerForEvents()
    }

    override fun onDestroy() {
        unregisterForEvents()
        super.onDestroy()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(event: MangaEvent) {
        manga = event.manga
        start(GET_MANGA_SYNC)
    }

    fun getSearchResultsObservable(): Observable<List<MangaSync>> {
        return query?.let { query ->
            val observable: Observable<List<MangaSync>>
            if (query.startsWith(PREFIX_MY)) {
                val realQuery = query.substring(PREFIX_MY.length).toLowerCase().trim()
                observable = myAnimeList.getList()
                        .flatMap { Observable.from(it) }
                        .filter { it.title.toLowerCase().contains(realQuery) }
                        .toList()
            } else {
                observable = myAnimeList.search(query)
            }
            observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        } ?: Observable.error(Exception("Null query"))

    }

    fun getRefreshObservable(): Observable<PutResult> {
        return mangaSync?.let { mangaSync ->
            myAnimeList.getList()
                    .flatMap { myList ->
                        for (myManga in myList) {
                            if (myManga.remote_id == mangaSync.remote_id) {
                                mangaSync.copyPersonalFrom(myManga)
                                mangaSync.total_chapters = myManga.total_chapters
                                return@flatMap Observable.just(mangaSync)
                            }
                        }
                        Observable.error<MangaSync>(Exception("Could not find manga"))
                    }
                    .flatMap { db.insertMangaSync(it).asRxObservable() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
        } ?: Observable.error(Exception("Not found"))
    }

    private fun updateRemote() {
        mangaSync?.let { mangaSync ->
            add(myAnimeList.update(mangaSync)
                    .flatMap { response -> db.insertMangaSync(mangaSync).asRxObservable() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ next -> },
                            { error ->
                                Timber.e(error.message)
                                // Restart on error to set old values
                                start(GET_MANGA_SYNC)
                            }))
        }
    }

    fun searchManga(query: String) {
        if (query.isNullOrEmpty() || query == this.query)
            return

        this.query = query
        start(GET_SEARCH_RESULTS)
    }

    fun restartSearch() {
        query = null
        stop(GET_SEARCH_RESULTS)
    }

    fun registerManga(sync: MangaSync) {
        sync.manga_id = manga.id
        add(myAnimeList.bind(sync)
                .flatMap { response ->
                    if (response.isSuccessful) {
                        db.insertMangaSync(sync).asRxObservable()
                    } else {
                        Observable.error(Exception("Could not bind manga"))
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ },
                        { error -> context.toast(error.message) }))
    }

    fun getAllStatus(): List<String> {
        return listOf(context.getString(R.string.reading),
                context.getString(R.string.completed),
                context.getString(R.string.on_hold),
                context.getString(R.string.dropped),
                context.getString(R.string.plan_to_read))
    }

    fun getIndexFromStatus(): Int {
        return mangaSync?.let { mangaSync ->
            if (mangaSync.status == 6) 4 else mangaSync.status - 1
        } ?: 0
    }

    fun setStatus(index: Int) {
        mangaSync?.status = if (index == 4) 6 else index + 1
        updateRemote()
    }

    fun setScore(score: Int) {
        mangaSync?.score = score.toFloat()
        updateRemote()
    }

    fun setLastChapterRead(chapterNumber: Int) {
        mangaSync?.last_chapter_read = chapterNumber
        updateRemote()
    }

    fun refresh() {
        if (mangaSync != null) {
            start(REFRESH)
        }
    }

}
