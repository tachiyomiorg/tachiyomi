package eu.kanade.tachiyomi.ui.manga.info

import android.os.Bundle
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.base.Source
import eu.kanade.tachiyomi.event.ChapterCountEvent
import eu.kanade.tachiyomi.event.MangaEvent
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import javax.inject.Inject

/**
 * Presenter of MangaInfoFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class MangaInfoPresenter : BasePresenter<MangaInfoFragment>() {

    /**
     * Active manga.
     */
    lateinit var manga: Manga
        private set

    /**
     * Source of the manga.
     */
    lateinit var source: Source
        private set

    /**
     * Used to connect to database.
     */
    @Inject lateinit var db: DatabaseHelper

    /**
     * Used to connect to different manga sources.
     */
    @Inject lateinit var sourceManager: SourceManager

    /**
     * Used to connect to cache.
     */
    @Inject lateinit var coverCache: CoverCache

    /**
     * Count of chapters.
     */
    private var count = -1

    /**
     * The id of the restartable.
     */
    private val GET_MANGA = 1

    /**
     * The id of the restartable.
     */
    private val GET_CHAPTER_COUNT = 2

    /**
     * The id of the restartable.
     */
    private val FETCH_MANGA_INFO = 3

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Notify the view a manga is available or has changed.
        startableLatestCache(GET_MANGA,
                { Observable.just(manga) },
                { view, manga -> view.onNextManga(manga, source) })

        // Update chapter count.
        startableLatestCache(GET_CHAPTER_COUNT,
                { Observable.just(count) },
                { view, count -> view.setChapterCount(count) })

        // Fetch manga info from source.
        startableFirst(FETCH_MANGA_INFO,
                { fetchMangaObs() },
                { view, manga -> view.onFetchMangaDone() },
                { view, error -> view.onFetchMangaError() })

        // Listen for events.
        registerForEvents()
    }

    override fun onDestroy() {
        unregisterForEvents()
        super.onDestroy()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(event: MangaEvent) {
        manga = event.manga
        source = sourceManager.get(manga.source)!!
        refreshManga()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(event: ChapterCountEvent) {
        if (count != event.count) {
            count = event.count
            // Update chapter count
            start(GET_CHAPTER_COUNT)
        }
    }

    /**
     * Fetch manga information from source.
     */
    fun fetchMangaFromSource() {
        if (isUnsubscribed(FETCH_MANGA_INFO)) {
            start(FETCH_MANGA_INFO)
        }
    }

    /**
     * Fetch manga information from source.
     *
     * @return manga information.
     */
    private fun fetchMangaObs(): Observable<Manga> {
        return source.pullMangaFromNetwork(manga.url)
                .flatMap { networkManga ->
                    manga.copyFrom(networkManga)
                    db.insertManga(manga).executeAsBlocking()
                    Observable.just<Manga>(manga)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { refreshManga() }
    }

    /**
     * Update favorite status of manga, (removes / adds) manga (to / from) library.
     */
    fun toggleFavorite() {
        manga.favorite = !manga.favorite
        onMangaFavoriteChange(manga.favorite)
        db.insertManga(manga).executeAsBlocking()
        refreshManga()
    }

    /**
     * (Removes / Saves) cover depending on favorite status.
     *
     * @param isFavorite determines if manga is favorite or not.
     */
    private fun onMangaFavoriteChange(isFavorite: Boolean) {
        if (isFavorite) {
            coverCache.save(manga.thumbnail_url, source.glideHeaders)
        } else {
            coverCache.deleteCoverFromCache(manga.thumbnail_url)
        }
    }

    /**
     * Refresh MangaInfo view.
     */
    private fun refreshManga() {
        start(GET_MANGA)
    }

}
