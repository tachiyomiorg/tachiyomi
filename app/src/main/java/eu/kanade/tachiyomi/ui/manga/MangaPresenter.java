package eu.kanade.tachiyomi.ui.manga;

import android.os.Bundle;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import eu.kanade.tachiyomi.data.database.DatabaseHelper;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import icepick.State;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MangaPresenter extends BasePresenter<MangaActivity> {

    @Inject DatabaseHelper db;

    @State long mangaId;

    private static final int DB_MANGA = 1;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        restartableLatestCache(DB_MANGA, this::getDbMangaObservable, MangaActivity::setManga);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Avoid new instances receiving wrong manga
        EventBus.getDefault().removeStickyEvent(Manga.class);
    }

    private Observable<Manga> getDbMangaObservable() {
        return db.getManga(mangaId).asRxObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(manga -> EventBus.getDefault().postSticky(manga));
    }

    public void queryManga(long mangaId) {
        this.mangaId = mangaId;
        start(DB_MANGA);
    }

}
