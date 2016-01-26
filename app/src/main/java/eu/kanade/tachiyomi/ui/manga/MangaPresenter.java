package eu.kanade.tachiyomi.ui.manga;

import android.os.Bundle;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import eu.kanade.tachiyomi.data.database.DatabaseHelper;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.event.MangaEvent;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import eu.kanade.tachiyomi.util.EventBusHook;
import icepick.State;
import rx.Observable;

public class MangaPresenter extends BasePresenter<MangaActivity> {

    @Inject DatabaseHelper db;

    @State Manga manga;

    private static final int GET_MANGA = 1;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        restartableLatestCache(GET_MANGA, this::getMangaObservable, MangaActivity::setManga);

        if (savedState == null)
            registerForStickyEvents();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Avoid new instances receiving wrong manga
        EventBus.getDefault().removeStickyEvent(MangaEvent.class);
    }

    private Observable<Manga> getMangaObservable() {
        return Observable.just(manga)
                .doOnNext(manga -> EventBus.getDefault().postSticky(new MangaEvent(manga)));
    }

    @EventBusHook
    public void onEventMainThread(Manga manga) {
        EventBus.getDefault().removeStickyEvent(manga);
        unregisterForEvents();
        this.manga = manga;
        start(GET_MANGA);
    }

}
