package eu.kanade.tachiyomi.ui.reader;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import eu.kanade.tachiyomi.data.database.DatabaseHelper;
import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.database.models.MangaSync;
import eu.kanade.tachiyomi.data.download.DownloadManager;
import eu.kanade.tachiyomi.data.download.model.Download;
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager;
import eu.kanade.tachiyomi.data.mangasync.base.MangaSyncService;
import eu.kanade.tachiyomi.data.preference.PreferencesHelper;
import eu.kanade.tachiyomi.data.source.SourceManager;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.data.sync.UpdateMangaSyncService;
import eu.kanade.tachiyomi.event.ReaderEvent;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import icepick.State;
import rx.Observable;
import rx.Subscription;
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
    @State Chapter activeChapter;
    @State int sourceId;
    @State int requestedPage;
    private Page currentPage;
    private Source source;
    private Chapter nextChapter;
    private Chapter previousChapter;
    private List<MangaSync> mangaSyncList;

    private PublishSubject<Page> retryPageSubject;
    private PublishSubject<Chapter> pageInitializerSubject;

    private boolean seamlessMode;
    private Subscription appenderSubscription;

    private static final int GET_PAGE_LIST = 1;
    private static final int GET_ADJACENT_CHAPTERS = 2;
    private static final int GET_MANGA_SYNC = 3;
    private static final int PRELOAD_NEXT_CHAPTER = 4;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            source = sourceManager.get(sourceId);
            initializeSubjects();
        }

        seamlessMode = prefs.seamlessMode();

        startableLatestCache(GET_ADJACENT_CHAPTERS, this::getAdjacentChaptersObservable,
                (view, pair) -> view.onAdjacentChapters(pair.first, pair.second));

        startable(PRELOAD_NEXT_CHAPTER, this::getPreloadNextChapterObservable,
                next -> {
                },
                error -> Timber.e("Error preloading chapter"));


        restartable(GET_MANGA_SYNC, () -> getMangaSyncObservable().subscribe());

        restartableLatestCache(GET_PAGE_LIST,
                () -> getPageListObservable(activeChapter),
                (view, chapter) -> view.onChapterReady(manga, activeChapter, currentPage),
                (view, error) -> view.onChapterError());

        if (savedState == null) {
            registerForEvents();
        }
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

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(ReaderEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        manga = event.getManga();
        source = event.getSource();
        sourceId = source.getId();
        initializeSubjects();
        loadChapter(event.getChapter());
        if (prefs.autoUpdateMangaSync()) {
            start(GET_MANGA_SYNC);
        }
    }

    private void initializeSubjects() {
        // Listen for pages initialization events
        pageInitializerSubject = PublishSubject.create();
        add(pageInitializerSubject
                .observeOn(Schedulers.io())
                .concatMap(chapter -> {
                    Observable observable;
                    if (chapter.isDownloaded()) {
                        File chapterDir = downloadManager.getAbsoluteChapterDirectory(source, manga, chapter);
                        observable = Observable.from(chapter.getPages())
                                .flatMap(page -> downloadManager.getDownloadedImage(page, chapterDir));
                    } else {
                        observable = source.getAllImageUrlsFromPageList(chapter.getPages())
                                .flatMap(source::getCachedImage, 2)
                                .doOnCompleted(() -> source.savePageList(chapter.url, chapter.getPages()));
                    }
                    return observable.doOnCompleted(() -> {
                        if (!seamlessMode && activeChapter == chapter) {
                            preloadNextChapter();
                        }
                    });
                })
                .subscribe());

        // Listen por retry events
        retryPageSubject = PublishSubject.create();
        add(retryPageSubject
                .observeOn(Schedulers.io())
                .flatMap(page -> page.getImageUrl() == null ?
                        source.getImageUrlFromPage(page) :
                        Observable.just(page))
                .flatMap(source::getCachedImage)
                .subscribe());
    }

    // Returns the page list of a chapter
    private Observable<Chapter> getPageListObservable(Chapter chapter) {
        return (chapter.isDownloaded() ?
                // Fetch the page list from disk
                Observable.just(downloadManager.getSavedPageList(source, manga, chapter)) :
                // Fetch the page list from cache or fallback to network
                source.getCachedPageListOrPullFromNetwork(chapter.url)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
        ).map(pages -> {
            for (Page page : pages) {
                page.setChapter(chapter);
            }
            chapter.setPages(pages);
            if (requestedPage >= -1 || currentPage == null) {
                if (requestedPage == -1) {
                    currentPage = pages.get(pages.size() - 1);
                } else {
                    currentPage = pages.get(requestedPage);
                }
            }
            requestedPage = -2;
            pageInitializerSubject.onNext(chapter);
            return chapter;
        });
    }

    private Observable<Pair<Chapter, Chapter>> getAdjacentChaptersObservable() {
        return Observable.zip(
                db.getPreviousChapter(activeChapter).asRxObservable().take(1),
                db.getNextChapter(activeChapter).asRxObservable().take(1),
                Pair::create)
                .doOnNext(pair -> {
                    previousChapter = pair.first;
                    nextChapter = pair.second;
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    // Preload the first pages of the next chapter. Only for non seamless mode
    private Observable<Page> getPreloadNextChapterObservable() {
        return source.getCachedPageListOrPullFromNetwork(nextChapter.url)
                .flatMap(pages -> {
                    nextChapter.setPages(pages);
                    int pagesToPreload = Math.min(pages.size(), 5);
                    return Observable.from(pages).take(pagesToPreload);
                })
                // Preload up to 5 images
                .concatMap(page -> page.getImageUrl() == null ?
                        source.getImageUrlFromPage(page) :
                        Observable.just(page))
                // Download the first image
                .concatMap(page -> page.getPageNumber() == 0 ?
                        source.getCachedImage(page) :
                        Observable.just(page))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(this::stopPreloadingNextChapter);
    }

    private Observable<List<MangaSync>> getMangaSyncObservable() {
        return db.getMangasSync(manga).asRxObservable()
                .take(1)
                .doOnNext(mangaSync -> this.mangaSyncList = mangaSync);
    }

    private void loadChapter(Chapter chapter) {
        loadChapter(chapter, 0);
    }

    // Loads the given chapter
    private void loadChapter(Chapter chapter, int requestedPage) {
        if (seamlessMode) {
            if (appenderSubscription != null)
                remove(appenderSubscription);
        } else {
            stopPreloadingNextChapter();
        }

        this.activeChapter = chapter;
        chapter.status = isChapterDownloaded(chapter) ? Download.DOWNLOADED : Download.NOT_DOWNLOADED;

        // If the chapter is partially read, set the starting page to the last the user read
        if (!chapter.read && chapter.last_page_read != 0)
            this.requestedPage = chapter.last_page_read;
        else
            this.requestedPage = requestedPage;

        // Reset next and previous chapter. They have to be fetched again
        nextChapter = null;
        previousChapter = null;

        start(GET_PAGE_LIST);
        start(GET_ADJACENT_CHAPTERS);
    }

    public void setActiveChapter(Chapter chapter) {
        onChapterLeft();
        this.activeChapter = chapter;
        nextChapter = null;
        previousChapter = null;
        start(GET_ADJACENT_CHAPTERS);
    }

    public void appendNextChapter() {
        if (nextChapter == null)
            return;

        if (appenderSubscription != null)
            remove(appenderSubscription);

        nextChapter.status = isChapterDownloaded(nextChapter) ? Download.DOWNLOADED : Download.NOT_DOWNLOADED;

        appenderSubscription = getPageListObservable(nextChapter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(deliverLatestCache())
                .subscribe(split((view, chapter) -> {
                    view.onAppendChapter(chapter);
                }, (view, error) -> {
                    view.onChapterAppendError();
                }));

        add(appenderSubscription);
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
        List<Page> pages = activeChapter.getPages();
        if (pages == null)
            return;

        // Get the last page read
        int activePageNumber = activeChapter.last_page_read;

        // Just in case, avoid out of index exceptions
        if (activePageNumber >= pages.size()) {
            activePageNumber = pages.size() - 1;
        }
        Page activePage = pages.get(activePageNumber);

        // Cache current page list progress for online chapters to allow a faster reopen
        if (!activeChapter.isDownloaded()) {
            source.savePageList(activeChapter.url, pages);
        }

        // Save current progress of the chapter. Mark as read if the chapter is finished
        if (activePage.isLastPage()) {
            activeChapter.read = true;

            // Check if user want to delete manga after reading it.
            if (prefs.deleteChapterAfterReading()) {
                // Check if the chapter is downloaded.
                if (activeChapter.isDownloaded()) {
                    // Delete chapter.
                    Observable<Chapter> chapterObservable = Observable.just(activeChapter);
                    onDelete(chapterObservable, manga);
                }
            }
        }
        db.insertChapter(activeChapter).asRxObservable().subscribe();
    }

    public int getMangaSyncChapterToUpdate() {
        if (activeChapter.getPages() == null || mangaSyncList == null || mangaSyncList.isEmpty())
            return 0;

        int lastChapterReadLocal = 0;
        // If the current chapter has been read, we check with this one
        if (activeChapter.read)
            lastChapterReadLocal = (int) Math.floor(activeChapter.chapter_number);
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

    public void setCurrentPage(Page currentPage) {
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
            if (nextChapter.getPages() != null)
                source.savePageList(nextChapter.url, nextChapter.getPages());
        }
    }

    /**
     * Start deleting chapter
     *
     * @param chapters selected chapters
     * @param manga    manga that belongs to chapter
     * @return success of deletion.
     */
    protected boolean onDelete(Observable<Chapter> chapters, Manga manga) {
        Observable<Chapter> observable = chapters
                .concatMap(chapter -> {
                    deleteChapter(chapter, manga);
                    return Observable.just(chapter);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(chapter -> {
                    chapter.status = Download.NOT_DOWNLOADED;
                });

        deleteChapters(observable);

        return true;
    }

    /**
     * Delete selected chapter
     *
     * @param chapter chapter that is selected
     * @param manga   manga that belongs to chapter
     */
    public void deleteChapter(Chapter chapter, Manga manga) {
        Source source = sourceManager.get(manga.source);
        downloadManager.deleteChapter(source, manga, chapter);
    }

    /**
     * Delete selected chapter observable
     *
     * @param selectedChapters chapter that are selected
     */
    public void deleteChapters(Observable<Chapter> selectedChapters) {
        add(selectedChapters
                .subscribe(chapter -> {
                    downloadManager.getQueue().remove(chapter);
                }, error -> {
                    Timber.e(error.getMessage());
                }));
    }

    public void updateMangaViewer(int viewer) {
        manga.viewer = viewer;
        db.insertManga(manga).executeAsBlocking();
    }

    public Manga getManga() {
        return manga;
    }

    public Page getCurrentPage() {
        return currentPage;
    }

    public boolean isSeamlessMode() {
        return seamlessMode;
    }
}
