package eu.kanade.tachiyomi.ui.catalogue;

import android.os.Bundle;
import android.text.TextUtils;

import com.pushtorefresh.storio.sqlite.operations.put.PutResult;

import java.util.List;

import javax.inject.Inject;

import eu.kanade.tachiyomi.data.cache.CoverCache;
import eu.kanade.tachiyomi.data.database.DatabaseHelper;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.preference.PreferencesHelper;
import eu.kanade.tachiyomi.data.source.SourceManager;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.model.MangasPage;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import eu.kanade.tachiyomi.util.RxPager;
import icepick.State;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public class CataloguePresenter extends BasePresenter<CatalogueFragment> {

    @Inject SourceManager sourceManager;
    @Inject DatabaseHelper db;
    @Inject CoverCache coverCache;
    @Inject PreferencesHelper prefs;

    private Source source;
    @State int sourceId;

    private String query;

    private RxPager<Manga> pager;
    private MangasPage lastMangasPage;

    private PublishSubject<List<Manga>> mangaDetailSubject;

    private boolean isListMode;

    private static final int GET_MANGA_LIST = 1;
    private static final int GET_MANGA_DETAIL = 2;
    private static final int GET_MANGA_PAGE = 3;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            onProcessRestart();
        }

        mangaDetailSubject = PublishSubject.create();

        pager = new RxPager<>();

        restartableReplay(GET_MANGA_LIST,
                pager::results,
                (view, pair) -> view.onAddPage(pair.first, pair.second));

        restartableFirst(GET_MANGA_PAGE,
                () -> pager.request(page -> getMangasPageObservable(page + 1)),
                (view, next) -> {},
                (view, error) -> view.onAddPageError());

        restartableLatestCache(GET_MANGA_DETAIL,
                () -> mangaDetailSubject
                        .observeOn(Schedulers.io())
                        .flatMap(Observable::from)
                        .filter(manga -> !manga.initialized)
                        .concatMap(this::getMangaDetails)
                        .onBackpressureBuffer()
                        .observeOn(AndroidSchedulers.mainThread()),
                CatalogueFragment::updateImage,
                (view, error) -> Timber.e(error.getMessage()));

        add(prefs.catalogueAsList().asObservable()
                .subscribe(this::setDisplayMode));
    }

    private void onProcessRestart() {
        source = sourceManager.get(sourceId);
        stop(GET_MANGA_LIST);
        stop(GET_MANGA_DETAIL);
        stop(GET_MANGA_PAGE);
    }

    private void setDisplayMode(boolean asList) {
        this.isListMode = asList;
        if (asList) {
            stop(GET_MANGA_DETAIL);
        } else {
            start(GET_MANGA_DETAIL);
        }
    }

    public void startRequesting(Source source) {
        this.source = source;
        sourceId = source.getId();
        restartRequest(null);
    }

    public void restartRequest(String query) {
        this.query = query;
        stop(GET_MANGA_PAGE);
        lastMangasPage = null;

        if (!isListMode) {
            start(GET_MANGA_DETAIL);
        }
        start(GET_MANGA_LIST);
        start(GET_MANGA_PAGE);
    }

    public void requestNext() {
        if (hasNextPage()) {
            start(GET_MANGA_PAGE);
        }
    }

    private Observable<List<Manga>> getMangasPageObservable(int page) {
        MangasPage nextMangasPage = new MangasPage(page);
        if (page != 1) {
            nextMangasPage.url = lastMangasPage.nextPageUrl;
        }

        Observable<MangasPage> obs = !TextUtils.isEmpty(query) ?
            source.searchMangasFromNetwork(nextMangasPage, query) :
            source.pullPopularMangasFromNetwork(nextMangasPage);

        return obs.subscribeOn(Schedulers.io())
                .doOnNext(mangasPage -> lastMangasPage = mangasPage)
                .flatMap(mangasPage -> Observable.from(mangasPage.mangas))
                .map(this::networkToLocalManga)
                .toList()
                .doOnNext(this::initializeMangas)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Manga networkToLocalManga(Manga networkManga) {
        Manga localManga = db.getManga(networkManga.url, source.getId()).executeAsBlocking();
        if (localManga == null) {
            PutResult result = db.insertManga(networkManga).executeAsBlocking();
            networkManga.id = result.insertedId();
            localManga = networkManga;
        }
        return localManga;
    }

    public void initializeMangas(List<Manga> mangas) {
        mangaDetailSubject.onNext(mangas);
    }

    private Observable<Manga> getMangaDetails(final Manga manga) {
        return source.pullMangaFromNetwork(manga.url)
                .flatMap(networkManga -> {
                    manga.copyFrom(networkManga);
                    db.insertManga(manga).executeAsBlocking();
                    return Observable.just(manga);
                })
                .onErrorResumeNext(error -> Observable.just(manga));
    }

    public Source getSource() {
        return source;
    }

    public boolean hasNextPage() {
        return lastMangasPage != null && lastMangasPage.nextPageUrl != null;
    }

    public boolean isValidSource(Source source) {
        if (!source.isLoginRequired() || source.isLogged())
            return true;

        return !(prefs.getSourceUsername(source).equals("")
                || prefs.getSourcePassword(source).equals(""));
    }

    public List<Source> getEnabledSources() {
        // TODO filter by enabled source
        return sourceManager.getSources();
    }

    public void changeMangaFavorite(Manga manga) {
        manga.favorite = !manga.favorite;
        db.insertManga(manga).executeAsBlocking();
    }

    public boolean isListMode() {
        return isListMode;
    }

    public void swapDisplayMode() {
        prefs.catalogueAsList().set(!isListMode);
    }

}
