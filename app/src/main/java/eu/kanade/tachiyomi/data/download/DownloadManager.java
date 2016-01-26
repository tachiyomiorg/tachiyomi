package eu.kanade.tachiyomi.data.download;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.download.model.Download;
import eu.kanade.tachiyomi.data.download.model.DownloadQueue;
import eu.kanade.tachiyomi.data.preference.PreferencesHelper;
import eu.kanade.tachiyomi.data.source.SourceManager;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.event.DownloadChaptersEvent;
import eu.kanade.tachiyomi.util.DiskUtils;
import eu.kanade.tachiyomi.util.DynamicConcurrentMergeOperator;
import eu.kanade.tachiyomi.util.UrlUtil;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public class DownloadManager {

    private Context context;
    private SourceManager sourceManager;
    private PreferencesHelper preferences;
    private Gson gson;

    private PublishSubject<List<Download>> downloadsQueueSubject;
    private BehaviorSubject<Boolean> runningSubject;
    private Subscription downloadsSubscription;

    private BehaviorSubject<Integer> threadsSubject;
    private Subscription threadsSubscription;

    private DownloadQueue queue;
    private volatile boolean isRunning;

    public static final String PAGE_LIST_FILE = "index.json";

    public DownloadManager(Context context, SourceManager sourceManager, PreferencesHelper preferences) {
        this.context = context;
        this.sourceManager = sourceManager;
        this.preferences = preferences;

        gson = new Gson();
        queue = new DownloadQueue();

        downloadsQueueSubject = PublishSubject.create();
        runningSubject = BehaviorSubject.create();
        threadsSubject = BehaviorSubject.create();
    }

    private void initializeSubscriptions() {
        if (downloadsSubscription != null && !downloadsSubscription.isUnsubscribed())
            downloadsSubscription.unsubscribe();

        threadsSubscription = preferences.downloadThreads().asObservable()
                .subscribe(threadsSubject::onNext);

        downloadsSubscription = downloadsQueueSubject
                .flatMap(Observable::from)
                .lift(new DynamicConcurrentMergeOperator<>(this::downloadChapter, threadsSubject))
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .map(download -> areAllDownloadsFinished())
                .subscribe(finished -> {
                    if (finished) {
                        DownloadService.stop(context);
                    }
                }, e -> Timber.e(e.getCause(), e.getMessage()));

        if (!isRunning) {
            isRunning = true;
            runningSubject.onNext(true);
        }
    }

    public void destroySubscriptions() {
        if (isRunning) {
            isRunning = false;
            runningSubject.onNext(false);
        }

        if (downloadsSubscription != null && !downloadsSubscription.isUnsubscribed()) {
            downloadsSubscription.unsubscribe();
            downloadsSubscription = null;
        }

        if (threadsSubscription != null && !threadsSubscription.isUnsubscribed()) {
            threadsSubscription.unsubscribe();
        }

    }

    // Create a download object for every chapter in the event and add them to the downloads queue
    public void onDownloadChaptersEvent(DownloadChaptersEvent event) {
        final Manga manga = event.getManga();
        final Source source = sourceManager.get(manga.source);

        // Used to avoid downloading chapters with the same name
        final List<String> addedChapters = new ArrayList<>();
        final List<Download> pending = new ArrayList<>();

        for (Chapter chapter : event.getChapters()) {
            if (addedChapters.contains(chapter.name))
                continue;

            addedChapters.add(chapter.name);
            Download download = new Download(source, manga, chapter);

            if (!prepareDownload(download)) {
                queue.add(download);
                pending.add(download);
            }
        }
        if (isRunning) downloadsQueueSubject.onNext(pending);
    }

    // Public method to check if a chapter is downloaded
    public boolean isChapterDownloaded(Source source, Manga manga, Chapter chapter) {
        File directory = getAbsoluteChapterDirectory(source, manga, chapter);
        if (!directory.exists())
            return false;

        List<Page> pages = getSavedPageList(source, manga, chapter);
        return isChapterDownloaded(directory, pages);
    }

    // Prepare the download. Returns true if the chapter is already downloaded
    private boolean prepareDownload(Download download) {
        // If the chapter is already queued, don't add it again
        for (Download queuedDownload : queue) {
            if (download.chapter.id.equals(queuedDownload.chapter.id))
                return true;
        }

        // Add the directory to the download object for future access
        download.directory = getAbsoluteChapterDirectory(download);

        // If the directory doesn't exist, the chapter isn't downloaded.
        if (!download.directory.exists()) {
            return false;
        }

        // If the page list doesn't exist, the chapter isn't downloaded
        List<Page> savedPages = getSavedPageList(download);
        if (savedPages == null)
            return false;

        // Add the page list to the download object for future access
        download.pages = savedPages;

        // If the number of files matches the number of pages, the chapter is downloaded.
        // We have the index file, so we check one file more
        return isChapterDownloaded(download.directory, download.pages);
    }

    // Check that all the images are downloaded
    private boolean isChapterDownloaded(File directory, List<Page> pages) {
        return pages != null && !pages.isEmpty() && pages.size() + 1 == directory.listFiles().length;
    }

    // Download the entire chapter
    private Observable<Download> downloadChapter(Download download) {
        try {
            DiskUtils.createDirectory(download.directory);
        } catch (IOException e) {
            return Observable.error(e);
        }

        Observable<List<Page>> pageListObservable = download.pages == null ?
                // Pull page list from network and add them to download object
                download.source
                        .pullPageListFromNetwork(download.chapter.url)
                        .doOnNext(pages -> download.pages = pages)
                        .doOnNext(pages -> savePageList(download)) :
                // Or if the page list already exists, start from the file
                Observable.just(download.pages);

        return Observable.defer(() -> pageListObservable
                .doOnNext(pages -> {
                    download.downloadedImages = 0;
                    download.setStatus(Download.DOWNLOADING);
                })
                // Get all the URLs to the source images, fetch pages if necessary
                .flatMap(download.source::getAllImageUrlsFromPageList)
                // Start downloading images, consider we can have downloaded images already
                .concatMap(page -> getOrDownloadImage(page, download))
                // Do after download completes
                .doOnCompleted(() -> onDownloadCompleted(download))
                .toList()
                .map(pages -> download)
                // If the page list threw, it will resume here
                .onErrorResumeNext(error -> {
                    download.setStatus(Download.ERROR);
                    return Observable.just(download);
                }))
                .subscribeOn(Schedulers.io());
    }

    // Get the image from the filesystem if it exists or download from network
    private Observable<Page> getOrDownloadImage(final Page page, Download download) {
        // If the image URL is empty, do nothing
        if (page.getImageUrl() == null)
            return Observable.just(page);

        String filename = getImageFilename(page);
        File imagePath = new File(download.directory, filename);

        // If the image is already downloaded, do nothing. Otherwise download from network
        Observable<Page> pageObservable = isImageDownloaded(imagePath) ?
                Observable.just(page) :
                downloadImage(page, download.source, download.directory, filename);

        return pageObservable
                // When the image is ready, set image path, progress (just in case) and status
                .doOnNext(p -> {
                    page.setImagePath(imagePath.getAbsolutePath());
                    page.setProgress(100);
                    download.downloadedImages++;
                    page.setStatus(Page.READY);
                })
                // Mark this page as error and allow to download the remaining
                .onErrorResumeNext(e -> {
                    page.setProgress(0);
                    page.setStatus(Page.ERROR);
                    return Observable.just(page);
                });
    }

    // Save image on disk
    private Observable<Page> downloadImage(Page page, Source source, File directory, String filename) {
        page.setStatus(Page.DOWNLOAD_IMAGE);
        return source.getImageProgressResponse(page)
                .flatMap(resp -> {
                    try {
                        DiskUtils.saveBufferedSourceToDirectory(resp.body().source(), directory, filename);
                    } catch (Exception e) {
                        Timber.e(e.getCause(), e.getMessage());
                        return Observable.error(e);
                    }
                    return Observable.just(page);
                })
                .retry(2);
    }

    // Public method to get the image from the filesystem. It does NOT provide any way to download the image
    public Observable<Page> getDownloadedImage(final Page page, File chapterDir) {
        if (page.getImageUrl() == null) {
            page.setStatus(Page.ERROR);
            return Observable.just(page);
        }

        File imagePath = new File(chapterDir, getImageFilename(page));

        // When the image is ready, set image path, progress (just in case) and status
        if (isImageDownloaded(imagePath)) {
            page.setImagePath(imagePath.getAbsolutePath());
            page.setProgress(100);
            page.setStatus(Page.READY);
        } else {
            page.setStatus(Page.ERROR);
        }
        return Observable.just(page);
    }

    // Get the filename for an image given the page
    private String getImageFilename(Page page) {
        String url = page.getImageUrl();
        int number = page.getPageNumber() + 1;
        // Try to preserve file extension
        if (UrlUtil.isJpg(url)) {
            return number + ".jpg";
        } else if (UrlUtil.isPng(url)) {
            return number + ".png";
        } else if (UrlUtil.isGif(url)) {
            return number + ".gif";
        }
        return Uri.parse(url).getLastPathSegment().replaceAll("[^\\sa-zA-Z0-9.-]", "_");
    }

    private boolean isImageDownloaded(File imagePath) {
        return imagePath.exists();
    }

    // Called when a download finishes. This doesn't mean the download was successful, so we check it
    private void onDownloadCompleted(final Download download) {
        checkDownloadIsSuccessful(download);
        savePageList(download);
    }

    private void checkDownloadIsSuccessful(final Download download) {
        int actualProgress = 0;
        int status = Download.DOWNLOADED;
        // If any page has an error, the download result will be error
        for (Page page : download.pages) {
            actualProgress += page.getProgress();
            if (page.getStatus() != Page.READY) status = Download.ERROR;
        }
        // Ensure that the chapter folder has all the images
        if (!isChapterDownloaded(download.directory, download.pages)) {
            status = Download.ERROR;
        }
        download.totalProgress = actualProgress;
        download.setStatus(status);
        // Delete successful downloads from queue after notifying
        if (status == Download.DOWNLOADED) {
            queue.remove(download);
        }
    }

    // Return the page list from the chapter's directory if it exists, null otherwise
    public List<Page> getSavedPageList(Source source, Manga manga, Chapter chapter) {
        List<Page> pages = null;
        File chapterDir = getAbsoluteChapterDirectory(source, manga, chapter);
        File pagesFile = new File(chapterDir, PAGE_LIST_FILE);

        JsonReader reader = null;
        try {
            if (pagesFile.exists()) {
                reader = new JsonReader(new FileReader(pagesFile.getAbsolutePath()));
                Type collectionType = new TypeToken<List<Page>>() {}.getType();
                pages = gson.fromJson(reader, collectionType);
            }
        } catch (FileNotFoundException e) {
            Timber.e(e.getCause(), e.getMessage());
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException e) { /* Do nothing */ }
        }
        return pages;
    }

    // Shortcut for the method above
    private List<Page> getSavedPageList(Download download) {
        return getSavedPageList(download.source, download.manga, download.chapter);
    }

    // Save the page list to the chapter's directory
    public void savePageList(Source source, Manga manga, Chapter chapter, List<Page> pages) {
        File chapterDir = getAbsoluteChapterDirectory(source, manga, chapter);
        File pagesFile = new File(chapterDir, PAGE_LIST_FILE);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(pagesFile);
            out.write(gson.toJson(pages).getBytes());
            out.flush();
        } catch (IOException e) {
            Timber.e(e.getCause(), e.getMessage());
        } finally {
            if (out != null) try { out.close(); } catch (IOException e) { /* Do nothing */ }
        }
    }

    // Shortcut for the method above
    private void savePageList(Download download) {
        savePageList(download.source, download.manga, download.chapter, download.pages);
    }

    // Get the absolute path to the chapter directory
    public File getAbsoluteChapterDirectory(Source source, Manga manga, Chapter chapter) {
        String chapterRelativePath = source.getName() +
                File.separator +
                manga.title.replaceAll("[^\\sa-zA-Z0-9.-]", "_") +
                File.separator +
                chapter.name.replaceAll("[^\\sa-zA-Z0-9.-]", "_");

        return new File(preferences.getDownloadsDirectory(), chapterRelativePath);
    }

    // Shortcut for the method above
    private File getAbsoluteChapterDirectory(Download download) {
        return getAbsoluteChapterDirectory(download.source, download.manga, download.chapter);
    }

    public void deleteChapter(Source source, Manga manga, Chapter chapter) {
        File path = getAbsoluteChapterDirectory(source, manga, chapter);
        DiskUtils.deleteFiles(path);
    }

    public DownloadQueue getQueue() {
        return queue;
    }

    public boolean areAllDownloadsFinished() {
        for (Download download : queue) {
            if (download.getStatus() <= Download.DOWNLOADING)
                return false;
        }
        return true;
    }

    public boolean startDownloads() {
        if (queue.isEmpty())
            return false;

        if (downloadsSubscription == null)
            initializeSubscriptions();

        final List<Download> pending = new ArrayList<>();
        for (Download download : queue) {
            if (download.getStatus() != Download.DOWNLOADED) {
                if (download.getStatus() != Download.QUEUE) download.setStatus(Download.QUEUE);
                pending.add(download);
            }
        }
        downloadsQueueSubject.onNext(pending);

        return !pending.isEmpty();
    }

    public void stopDownloads() {
        destroySubscriptions();
        for (Download download : queue) {
            if (download.getStatus() == Download.DOWNLOADING) {
                download.setStatus(Download.ERROR);
            }
        }
    }

    public BehaviorSubject<Boolean> getRunningSubject() {
        return runningSubject;
    }

}
