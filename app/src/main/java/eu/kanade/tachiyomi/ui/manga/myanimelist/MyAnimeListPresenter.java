package eu.kanade.tachiyomi.ui.manga.myanimelist;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import javax.inject.Inject;

import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.database.DatabaseHelper;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.database.models.MangaSync;
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager;
import eu.kanade.tachiyomi.data.mangasync.services.MyAnimeList;
import eu.kanade.tachiyomi.event.MangaEvent;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import eu.kanade.tachiyomi.util.EventBusHook;
import eu.kanade.tachiyomi.util.ToastUtil;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MyAnimeListPresenter extends BasePresenter<MyAnimeListFragment> {

    @Inject DatabaseHelper db;
    @Inject MangaSyncManager syncManager;

    protected MyAnimeList myAnimeList;
    protected Manga manga;
    protected MangaSync mangaSync;

    private String query;

    private static final int GET_MANGA_SYNC = 1;
    private static final int GET_SEARCH_RESULTS = 2;
    private static final int REFRESH = 3;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            onProcessRestart();
        }

        myAnimeList = syncManager.getMyAnimeList();

        restartableLatestCache(GET_MANGA_SYNC,
                () -> db.getMangaSync(manga, myAnimeList).asRxObservable()
                        .doOnNext(mangaSync -> this.mangaSync = mangaSync)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()),
                MyAnimeListFragment::setMangaSync);

        restartableLatestCache(GET_SEARCH_RESULTS,
                () -> myAnimeList.search(query)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()),
                (view, results) -> {
                    view.setSearchResults(results);
                }, (view, error) -> {
                    Timber.e(error.getMessage());
                    view.setSearchResultsError();
                });

        restartableFirst(REFRESH,
                () -> myAnimeList.getList()
                        .flatMap(myList -> {
                            for (MangaSync myManga : myList) {
                                if (myManga.remote_id == mangaSync.remote_id) {
                                    mangaSync.copyPersonalFrom(myManga);
                                    mangaSync.total_chapters = myManga.total_chapters;
                                    return Observable.just(mangaSync);
                                }
                            }
                            return Observable.error(new Exception("Could not find manga"));
                        })
                        .flatMap(myManga -> db.insertMangaSync(myManga).asRxObservable())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()),
                (view, result) -> view.onRefreshDone(),
                (view, error) -> view.onRefreshError());

    }

    private void onProcessRestart() {
        stop(GET_MANGA_SYNC);
        stop(GET_SEARCH_RESULTS);
        stop(REFRESH);
    }

    @Override
    protected void onTakeView(MyAnimeListFragment view) {
        super.onTakeView(view);
        registerForStickyEvents();
    }

    @Override
    protected void onDropView() {
        unregisterForEvents();
        super.onDropView();
    }

    @EventBusHook
    public void onEventMainThread(MangaEvent event) {
        this.manga = event.manga;
        start(GET_MANGA_SYNC);
    }

    private void updateRemote() {
        add(myAnimeList.update(mangaSync)
                .flatMap(response -> db.insertMangaSync(mangaSync).asRxObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(next -> {},
                        error -> {
                            Timber.e(error.getMessage());
                            // Restart on error to set old values
                            start(GET_MANGA_SYNC);
                        }
                ));
    }

    public void searchManga(String query) {
        if (TextUtils.isEmpty(query) || query.equals(this.query))
            return;

        this.query = query;
        start(GET_SEARCH_RESULTS);
    }

    public void restartSearch() {
        this.query = null;
        stop(GET_SEARCH_RESULTS);
    }

    public void registerManga(MangaSync manga) {
        manga.manga_id = this.manga.id;
        add(myAnimeList.bind(manga)
                .flatMap(response -> {
                    if (response.isSuccessful()) {
                        return db.insertMangaSync(manga).asRxObservable();
                    }
                    return Observable.error(new Exception("Could not bind manga"));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(manga2 -> {},
                        error -> ToastUtil.showShort(getContext(), error.getMessage())));
    }

    public String[] getAllStatus(Context context) {
        return new String[] {
                context.getString(R.string.reading),
                context.getString(R.string.completed),
                context.getString(R.string.on_hold),
                context.getString(R.string.dropped),
                context.getString(R.string.plan_to_read)
        };
    }

    public int getIndexFromStatus() {
        return mangaSync.status == 6 ? 4 : mangaSync.status - 1;
    }

    public void setStatus(int index) {
        mangaSync.status = index == 4 ? 6 : index + 1;
        updateRemote();
    }

    public void setScore(int score) {
        mangaSync.score = score;
        updateRemote();
    }

    public void setLastChapterRead(int chapterNumber) {
        mangaSync.last_chapter_read = chapterNumber;
        updateRemote();
    }

    public void refresh() {
        if (mangaSync != null) {
            start(REFRESH);
        }
    }
}
