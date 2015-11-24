package eu.kanade.mangafeed.ui.manga.info;

import android.os.Bundle;

import javax.inject.Inject;

import eu.kanade.mangafeed.data.database.DatabaseHelper;
import eu.kanade.mangafeed.data.database.models.Manga;
import eu.kanade.mangafeed.event.ChapterCountEvent;
import eu.kanade.mangafeed.ui.base.presenter.BasePresenter;
import eu.kanade.mangafeed.util.EventBusHook;
import rx.Observable;

public class MangaInfoPresenter extends BasePresenter<MangaInfoFragment> {

    @Inject
    DatabaseHelper db;

    private Manga manga;
    private int count = -1;

    private static final int GET_MANGA = 1;
    private static final int GET_CHAPTER_COUNT = 2;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        restartableLatestCache(GET_MANGA,
                () -> Observable.just(manga),
                MangaInfoFragment::setMangaInfo);

        restartableLatestCache(GET_CHAPTER_COUNT,
                () -> Observable.just(count),
                MangaInfoFragment::setChapterCount);
    }

    @Override
    protected void onTakeView(MangaInfoFragment view) {
        super.onTakeView(view);
        registerForStickyEvents();
    }

    @Override
    protected void onDropView() {
        unregisterForEvents();
        super.onDropView();
    }

    @EventBusHook
    public void onEventMainThread(Manga manga) {
        this.manga = manga;
        start(GET_MANGA);
    }

    @EventBusHook
    public void onEventMainThread(ChapterCountEvent event) {
        if (count != event.getCount()) {
            count = event.getCount();
            start(GET_CHAPTER_COUNT);
        }
    }

    public void initFavoriteText() {
        if (getView() != null)
            getView().setFavoriteText(manga.favorite);
    }

    public void toggleFavorite() {
        manga.favorite = !manga.favorite;
        db.insertManga(manga).executeAsBlocking();
    }

}
