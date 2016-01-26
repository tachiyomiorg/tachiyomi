package eu.kanade.tachiyomi.ui.reader;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import eu.kanade.tachiyomi.data.database.DatabaseHelper;
import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.database.models.MangaSync;
import eu.kanade.tachiyomi.data.download.DownloadManager;
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager;
import eu.kanade.tachiyomi.data.mangasync.base.MangaSyncService;
import eu.kanade.tachiyomi.data.preference.PreferencesHelper;
import eu.kanade.tachiyomi.data.source.SourceManager;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.data.sync.UpdateMangaSyncService;
import eu.kanade.tachiyomi.event.ReaderEvent;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import eu.kanade.tachiyomi.util.EventBusHook;
import icepick.State;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public class ReaderPresenter extends BasePresenter<ReaderActivity> {

    @Inject PreferencesHelper prefs;
    @Inject DatabaseHelper db;
    @Inject DownloadManager downloadManager;
    @Inject MangaSyncManager syncManager;
    @Inject SourceManager sourceManager;

    @State Manga manga;
    @State Chapter chapter;
    @State int sourceId;
    @State boolean isDownloaded;
    @State int currentPage;
    private Source source;
    private Chapter nextChapter;
    private Chapter previousChapter;
    private List<Page> pageList;
    private List<Page> nextChapterPageList;
    private List<MangaSync> mangaSyncList;

    private PublishSubject<Page> retryPageSubject;

    private static final int GET_PAGE_LIST = 1;
    private static final int GET_PAGE_IMAGES = 2;
    private static final int GET_ADJACENT_CHAPTERS = 3;
    private static final int RETRY_IMAGES = 4;
    private static final int PRELOAD_NEXT_CHAPTER = 5;
    private static final int GET_MANGA_SYNC = 6;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            onProcessRestart();
        }

        retryPageSubject = PublishSubject.create();

        restartableLatestCache(PRELOAD_NEXT_CHAPTER,
                this::getPreloadNextChapterObservable,
                (view, pages) -> {},
                (view, error) -> Timber.e("An error occurred while preloading a chapter"));

        restartableLatestCache(GET_PAGE_IMAGES,
                this::getPageImagesObservable,
                (view, page) -> {},
                (view, error) -> Timber.e("An error occurred while downloading an image"));

        restartableLatestCache(GET_ADJACENT_CHAPTERS,
                this::getAdjacentChaptersObservable,
                (view, pair) -> view.onAdjacentChapters(pair.first, pair.second),
                (view, error) -> Timber.e("An error occurred while getting adjacent chapters"));

        restartableLatestCache(RETRY_IMAGES,
                this::getRetryPageObservable,
                (view, page) -> {},
                (view, error) -> Timber.e("An error occurred while downloading an image"));

        restartableLatestCache(GET_PAGE_LIST,
                () -> getPageListObservable()
                        .doOnNext(pages -> pageList = pages)
                        .doOnCompleted(() -> {
                            start(GET_ADJACENT_CHAPTERS);
                            start(GET_PAGE_IMAGES);
                            start(RETRY_IMAGES);
                        }),
                (view, pages) -> view.onChapterReady(pages, manga, chapter, currentPage),
                (view, error) -> view.onChapterError());

        restartableFirst(GET_MANGA_SYNC, this::getMangaSyncObservable,
                (view, mangaSync) -> {},
                (view, error) -> {});

        registerForStickyEvents();
    }

    @Override
    protected void onDestroy() {
        unregisterForEvents();
        super.onDestroy();
    }

    @Override
    protected void onSave(@NonNull Bundle state) {
        onChapterLeft();
        super.onSave(state);
    }

    private void onProcessRestart() {
        source = sourceManager.get(sourceId);

        // These are started by GET_PAGE_LIST, so we don't let them restart itselves
        stop(GET_PAGE_IMAGES);
        stop(GET_ADJACENT_CHAPTERS);
        stop(RETRY_IMAGES);
        stop(PRELOAD_NEXT_CHAPTER);
    }

    @EventBusHook
    public void onEventMainThread(ReaderEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        manga = event.getManga();
        source = event.getSource();
        sourceId = source.getId();
        loadChapter(event.getChapter());
        if (prefs.autoUpdateMangaSync()) {
            start(GET_MANGA_SYNC);
        }
    }

    // Returns the page list of a chapter
    private Observable<List<Page>> getPageListObservable() {
        return isDownloaded ?
                // Fetch the page list from disk
                Observable.just(downloadManager.getSavedPageList(source, manga, chapter)) :
                // Fetch the page list from cache or fallback to network
                source.getCachedPageListOrPullFromNetwork(chapter.url)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
    }

    // Get the chapter images from network or disk
    private Observable<Page> getPageImagesObservable() {
        Observable<Page> pageObservable;

        if (!isDownloaded) {
            pageObservable = source.getAllImageUrlsFromPageList(pageList)
                    .flatMap(source::getCachedImage, 2);
        } else {
            File chapterDir = downloadManager.getAbsoluteChapterDirectory(source, manga, chapter);
            pageObservable = Observable.from(pageList)
                    .flatMap(page -> downloadManager.getDownloadedImage(page, chapterDir));
        }
        return pageObservable.subscribeOn(Schedulers.io())
                .doOnCompleted(this::preloadNextChapter);
    }

    private Observable<Pair<Chapter, Chapter>> getAdjacentChaptersObservable() {
        return Observable.zip(
                db.getPreviousChapter(chapter).asRxObservable().take(1),
                db.getNextChapter(chapter).asRxObservable().take(1),
                Pair::create)
                .doOnNext(pair -> {
                    previousChapter = pair.first;
                    nextChapter = pair.second;
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    // Listen for retry page events
    private Observable<Page> getRetryPageObservable() {
        return retryPageSubject
                .observeOn(Schedulers.io())
                .flatMap(page -> page.getImageUrl() == null ?
                        source.getImageUrlFromPage(page) :
                        Observable.just(page))
                .flatMap(source::getCachedImage)
                .observeOn(AndroidSchedulers.mainThread());
    }

    // Preload the first pages of the next chapter
    private Observable<Page> getPreloadNextChapterObservable() {
        return source.getCachedPageListOrPullFromNetwork(nextChapter.url)
                .flatMap(pages -> {
                    nextChapterPageList = pages;
                    // Preload at most 5 pages
                    int pagesToPreload = Math.min(pages.size(), 5);
                    return Observable.from(pages).take(pagesToPreload);
                })
                .concatMap(page -> page.getImageUrl() == null ?
                        source.getImageUrlFromPage(page) :
                        Observable.just(page))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(this::stopPreloadingNextChapter);
    }

    private Observable<List<MangaSync>> getMangaSyncObservable() {
        return db.getMangasSync(manga).asRxObservable()
                .doOnNext(mangaSync -> this.mangaSyncList = mangaSync);
    }

    private void loadChapter(Chapter chapter) {
        loadChapter(chapter, 0);
    }

    // Loads the given chapter
    private void loadChapter(Chapter chapter, int requestedPage) {
        // Before loading the chapter, stop preloading (if it's working) and save current progress
        stopPreloadingNextChapter();

        this.chapter = chapter;
        isDownloaded = isChapterDownloaded(chapter);

        // If the chapter is partially read, set the starting page to the last the user read
        if (!chapter.read && chapter.last_page_read != 0)
            currentPage = chapter.last_page_read;
        else
            currentPage = requestedPage;

        // Reset next and previous chapter. They have to be fetched again
        nextChapter = null;
        previousChapter = null;
        nextChapterPageList = null;

        start(GET_PAGE_LIST);
    }

    // Check whether the given chapter is downloaded
    public boolean isChapterDownloaded(Chapter chapter) {
        return downloadManager.isChapterDownloaded(source, manga, chapter);
    }

    public void retryPage(Page page) {
        page.setStatus(Page.QUEUE);
        retryPageSubject.onNext(page);
    }

    // Called before loading another chapter or leaving the reader. It allows to do operations
    // over the chapter read like saving progress
    public void onChapterLeft() {
        if (pageList == null)
            return;

        // Cache current page list progress for online chapters to allow a faster reopen
        if (!isDownloaded)
            source.savePageList(chapter.url, pageList);

        // Save current progress of the chapter. Mark as read if the chapter is finished
        chapter.last_page_read = currentPage;
        if (isChapterFinished()) {
            chapter.read = true;
        }
        db.insertChapter(chapter).asRxObservable().subscribe();
    }

    // Check whether the chapter has been read
    private boolean isChapterFinished() {
        return !chapter.read && currentPage == pageList.size() - 1;
    }

    public int getMangaSyncChapterToUpdate() {
        if (pageList == null || mangaSyncList == null || mangaSyncList.isEmpty())
            return 0;

        int lastChapterReadLocal = 0;
        // If the current chapter has been read, we check with this one
        if (chapter.read)
            lastChapterReadLocal = (int) Math.floor(chapter.chapter_number);
        // If not, we check if the previous chapter has been read
        else if (previousChapter != null && previousChapter.read)
            lastChapterReadLocal = (int) Math.floor(previousChapter.chapter_number);

        // We know the chapter we have to check, but we don't know yet if an update is required.
        // This boolean is used to return 0 if no update is required
        boolean hasToUpdate = false;

        for (MangaSync mangaSync : mangaSyncList) {
            if (lastChapterReadLocal > mangaSync.last_chapter_read) {
                mangaSync.last_chapter_read = lastChapterReadLocal;
                mangaSync.update = true;
                hasToUpdate = true;
            }
        }
        return hasToUpdate ? lastChapterReadLocal : 0;
    }

    public void updateMangaSyncLastChapterRead() {
        for (MangaSync mangaSync : mangaSyncList) {
            MangaSyncService service = syncManager.getSyncService(mangaSync.sync_id);
            if (service.isLogged() && mangaSync.update) {
                UpdateMangaSyncService.start(getContext(), mangaSync);
            }
        }
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public boolean loadNextChapter() {
        if (hasNextChapter()) {
            onChapterLeft();
            loadChapter(nextChapter, 0);
            return true;
        }
        return false;
    }

    public boolean loadPreviousChapter() {
        if (hasPreviousChapter()) {
            onChapterLeft();
            loadChapter(previousChapter, -1);
            return true;
        }
        return false;
    }

    public boolean hasNextChapter() {
        return nextChapter != null;
    }

    public boolean hasPreviousChapter() {
        return previousChapter != null;
    }

    private void preloadNextChapter() {
        if (hasNextChapter() && !isChapterDownloaded(nextChapter)) {
            start(PRELOAD_NEXT_CHAPTER);
        }
    }

    private void stopPreloadingNextChapter() {
        if (!isUnsubscribed(PRELOAD_NEXT_CHAPTER)) {
            stop(PRELOAD_NEXT_CHAPTER);
            if (nextChapterPageList != null)
                source.savePageList(nextChapter.url, nextChapterPageList);
        }
    }

    public void updateMangaViewer(int viewer) {
        manga.viewer = viewer;
        db.insertManga(manga).executeAsBlocking();
    }

    public Manga getManga() {
        return manga;
    }

}
