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
import eu.kanade.tachiyomi.util.SharedData
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
     * The id of the restartable.
     */
    private val GET_MANGA = 1

    /**
     * The id of the restartable.
     */
    private val FETCH_MANGA_INFO = 2

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Notify the view a manga is available or has changed.
        startableLatestCache(GET_MANGA,
                { Observable.just(manga) },
                { view, manga -> view.onNextManga(manga, source) })

        // Fetch manga info from source.
        startableFirst(FETCH_MANGA_INFO,
                { fetchMangaObs() },
                { view, manga -> view.onFetchMangaDone() },
                { view, error -> view.onFetchMangaError() })

        manga = SharedData.get(MangaEvent::class.java)?.manga ?: return
        source = sourceManager.get(manga.source)!!
        refreshManga()

        // Update chapter count
        SharedData.get(ChapterCountEvent::class.java)?.let {
            it.observable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeLatestCache({ view, count -> view.setChapterCount(count) })
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
