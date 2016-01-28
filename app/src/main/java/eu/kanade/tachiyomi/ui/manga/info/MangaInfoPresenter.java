package eu.kanade.tachiyomi.ui.manga.info;

import android.os.Bundle;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import eu.kanade.tachiyomi.data.cache.CoverCache;
import eu.kanade.tachiyomi.data.database.DatabaseHelper;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.SourceManager;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.event.ChapterCountEvent;
import eu.kanade.tachiyomi.event.MangaEvent;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import eu.kanade.tachiyomi.util.EventBusHook;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MangaInfoPresenter extends BasePresenter<MangaInfoFragment> {

    /**
     * The id of the restartable.
     */
    private static final int GET_MANGA = 1;
    /**
     * The id of the restartable.
     */
    private static final int GET_CHAPTER_COUNT = 2;
    /**
     * The id of the restartable.
     */
    private static final int FETCH_MANGA_INFO = 3;
    /**
     * Source information
     */
    protected Source source;
    /**
     * Used to connect to database
     */
    @Inject DatabaseHelper db;
    /**
     * Used to connect to different manga sources
     */
    @Inject SourceManager sourceManager;
    /**
     * Used to connect to cache
     */
    @Inject CoverCache coverCache;
    /**
     * Selected manga information
     */
    private Manga manga;
    /**
     * Count of chapters
     */
    private int count = -1;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            onProcessRestart();
        }

        // Update manga cache
        restartableLatestCache(GET_MANGA,
                () -> Observable.just(manga),
                (view, manga) -> view.onNextManga(manga, source));

        // Update chapter count
        restartableLatestCache(GET_CHAPTER_COUNT,
                () -> Observable.just(count),
                MangaInfoFragment::setChapterCount);

        // Fetch manga info from source
        restartableFirst(FETCH_MANGA_INFO,
                this::fetchMangaObs,
                (view, manga) -> view.onFetchMangaDone(),
                (view, error) -> view.onFetchMangaError());

        // TODO reason?
        registerForStickyEvents();
    }

    /**
     * Called when savedState not null
     */
    private void onProcessRestart() {
        stop(GET_MANGA);
        stop(GET_CHAPTER_COUNT);
        stop(FETCH_MANGA_INFO);
    }

    @Override
    protected void onDestroy() {
        unregisterForEvents();
        super.onDestroy();
    }

    @EventBusHook
    public void onEventMainThread(MangaEvent event) {
        this.manga = event.manga;
        source = sourceManager.get(manga.source);
        refreshManga();
    }

    @EventBusHook
    public void onEventMainThread(ChapterCountEvent event) {
        if (count != event.getCount()) {
            count = event.getCount();
            start(GET_CHAPTER_COUNT);
        }
    }

    /**
     * Fetch manga info from source
     */
    public void fetchMangaFromSource() {
        if (isUnsubscribed(FETCH_MANGA_INFO)) {
            start(FETCH_MANGA_INFO);
        }
    }

    private Observable<Manga> fetchMangaObs() {
        return source.pullMangaFromNetwork(manga.url)
                .flatMap(networkManga -> {
                    manga.copyFrom(networkManga);
                    db.insertManga(manga).executeAsBlocking();
                    return Observable.just(manga);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(manga -> refreshManga());
    }

    public void toggleFavorite() {
        manga.favorite = !manga.favorite;
        onMangaFavoriteChange(manga.favorite);
        db.insertManga(manga).executeAsBlocking();
        refreshManga();
    }

    /**
     * Update cover with local file
     */
    public void editCoverWithLocalFile(File file, ImageView imageView) throws IOException {
        if (manga.favorite) {
            coverCache.copyToLocalCache(manga.thumbnail_url, file);
            coverCache.loadFromCache(imageView, file);
        }
    }

    private void onMangaFavoriteChange(boolean isFavorite) {
        if (isFavorite) {
            coverCache.save(manga.thumbnail_url, source.getGlideHeaders());
        } else {
            coverCache.deleteCoverFromCache(manga.thumbnail_url);
        }
    }

    public Manga getManga() {
        return manga;
    }

    // Used to refresh the view
    protected void refreshManga() {
        start(GET_MANGA);
    }

}
