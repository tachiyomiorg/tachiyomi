package eu.kanade.mangafeed.ui.manga.chapter;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import eu.kanade.mangafeed.data.database.DatabaseHelper;
import eu.kanade.mangafeed.data.database.models.Chapter;
import eu.kanade.mangafeed.data.database.models.Manga;
import eu.kanade.mangafeed.data.download.DownloadManager;
import eu.kanade.mangafeed.data.preference.PreferencesHelper;
import eu.kanade.mangafeed.data.source.SourceManager;
import eu.kanade.mangafeed.data.source.base.Source;
import eu.kanade.mangafeed.data.source.model.Page;
import eu.kanade.mangafeed.event.ChapterCountEvent;
import eu.kanade.mangafeed.event.DownloadChaptersEvent;
import eu.kanade.mangafeed.event.SourceMangaChapterEvent;
import eu.kanade.mangafeed.ui.base.presenter.BasePresenter;
import eu.kanade.mangafeed.util.EventBusHook;
import eu.kanade.mangafeed.util.PostResult;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ChaptersPresenter extends BasePresenter<ChaptersFragment> {

    @Inject
    DatabaseHelper db;
    @Inject
    SourceManager sourceManager;
    @Inject
    PreferencesHelper preferences;
    @Inject
    DownloadManager downloadManager;

    private Manga manga;
    private Source source;
    private boolean sortOrderAToZ = true;
    private boolean onlyUnread = true;

    private static final int DB_CHAPTERS = 1;
    private static final int ONLINE_CHAPTERS = 2;

    private Subscription markReadSubscription;
    private Subscription downloadSubscription;
    private Subscription deleteSubscription;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        restartableLatestCache(DB_CHAPTERS,
                this::getDbChaptersObs,
                (view, chapters) -> {
                    view.onNextChapters(chapters);
                    EventBus.getDefault().postSticky(new ChapterCountEvent(chapters.size()));
                }
        );

        restartableLatestCache(ONLINE_CHAPTERS,
                this::getOnlineChaptersObs,
                (view, result) -> view.onNextOnlineChapters()
        );
    }

    @Override
    protected void onTakeView(ChaptersFragment view) {
        super.onTakeView(view);
        registerForStickyEvents();
    }

    @Override
    protected void onDropView() {
        unregisterForEvents();
        super.onDropView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().removeStickyEvent(ChapterCountEvent.class);
    }

    @EventBusHook
    public void onEventMainThread(Manga manga) {
        if (this.manga == null) {
            this.manga = manga;
            source = sourceManager.get(manga.source);
            start(DB_CHAPTERS);

            // Get chapters if it's an online source
            if (getView() != null && getView().isOnlineManga()) {
                refreshChapters();
            }
        }
    }

    public void refreshChapters() {
        if (getView() != null)
            getView().setSwipeRefreshing();

        start(ONLINE_CHAPTERS);
    }

    private Observable<List<Chapter>> getDbChaptersObs() {
        return db.getChapters(manga.id, sortOrderAToZ, onlyUnread).createObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<PostResult> getOnlineChaptersObs() {
        return source
                .pullChaptersFromNetwork(manga.url)
                .subscribeOn(Schedulers.io())
                .flatMap(chapters -> db.insertOrRemoveChapters(manga, chapters))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void onChapterClicked(Chapter chapter) {
        EventBus.getDefault().postSticky(new SourceMangaChapterEvent(source, manga, chapter));
    }

    public void markChaptersRead(Observable<Chapter> selectedChapters, boolean read) {
        add(markReadSubscription = selectedChapters
                .subscribeOn(Schedulers.io())
                .map(chapter -> {
                    chapter.read = read;
                    if (!read) chapter.last_page_read = 0;
                    return chapter;
                })
                .toList()
                .flatMap(chapters -> db.insertChapters(chapters).createObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> remove(markReadSubscription))
                .subscribe(result -> {
                }));
    }

    public void downloadChapters(Observable<Chapter> selectedChapters) {
        add(downloadSubscription = selectedChapters
                .toList()
                .subscribe(chapters -> {
                    EventBus.getDefault().postSticky(new DownloadChaptersEvent(manga, chapters));
                    remove(downloadSubscription);
                }));
    }

    public void deleteChapters(Observable<Chapter> selectedChapters) {
        deleteSubscription = selectedChapters
                .doOnCompleted(() -> remove(deleteSubscription))
                .subscribe(chapter -> {
                    downloadManager.deleteChapter(source, manga, chapter);
                    chapter.downloaded = Chapter.NOT_DOWNLOADED;
                });
    }

    public void checkIsChapterDownloaded(Chapter chapter) {
        File dir = downloadManager.getAbsoluteChapterDirectory(source, manga, chapter);
        List<Page> pageList = downloadManager.getSavedPageList(source, manga, chapter);

        if (pageList != null && pageList.size() + 1 == dir.listFiles().length) {
            chapter.downloaded = Chapter.DOWNLOADED;
        } else {
            chapter.downloaded = Chapter.NOT_DOWNLOADED;
        }
    }

    public void initSortIcon() {
        if (getView() != null) {
            getView().setSortIcon(sortOrderAToZ);//TODO do we need save order for manga?
        }
    }

    public void initReadCb(){
        if (getView() != null) {
            getView().setReadFilter(onlyUnread);//TODO do we need save filter for manga?
        }
    }

    public void revertSortOrder() {
        sortOrderAToZ = !sortOrderAToZ;
        initSortIcon();
        start(DB_CHAPTERS);
    }

    public void setReadFilter(boolean onlyUnread) {
        this.onlyUnread = onlyUnread;
        initReadCb();
        start(DB_CHAPTERS);
    }
}
