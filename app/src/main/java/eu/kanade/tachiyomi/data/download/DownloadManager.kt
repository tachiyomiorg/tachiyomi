package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.base.Source
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.util.DiskUtils
import eu.kanade.tachiyomi.util.DynamicConcurrentMergeOperator
import eu.kanade.tachiyomi.util.UrlUtil
import eu.kanade.tachiyomi.util.saveImageTo
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context, private val sourceManager: SourceManager, private val preferences: PreferencesHelper) {

    private val gson = Gson()

    private val downloadsQueueSubject = PublishSubject.create<List<Download>>()
    val runningSubject = BehaviorSubject.create<Boolean>()
    private var downloadsSubscription: Subscription? = null

    val downloadNotifier by lazy { DownloadNotifier(context) }

    private val threadsSubject = BehaviorSubject.create<Int>()
    private var threadsSubscription: Subscription? = null

    val queue = DownloadQueue()

    val imageFilenameRegex = "[^\\sa-zA-Z0-9.-]".toRegex()

    val PAGE_LIST_FILE = "index.json"

    @Volatile var isRunning: Boolean = false
        private set

    private fun initializeSubscriptions() {

        downloadsSubscription?.unsubscribe()

        threadsSubscription = preferences.downloadThreads().asObservable()
                .subscribe {
                    threadsSubject.onNext(it)
                    downloadNotifier.multipleDownloadThreads = it > 1
                }

        downloadsSubscription = downloadsQueueSubject.flatMap { Observable.from(it) }
                .lift(DynamicConcurrentMergeOperator<Download, Download>({ downloadChapter(it) }, threadsSubject))
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Delete successful downloads from queue
                    if (it.status == Download.DOWNLOADED) {
                        // remove downloaded chapter from queue
                        queue.del(it)
                        downloadNotifier.onProgressChange(queue)
                    }
                    if (areAllDownloadsFinished()) {
                        DownloadService.stop(context)
                    }
                }, { e ->
                    DownloadService.stop(context)
                    Timber.e(e, e.message)
                    downloadNotifier.onError(e.message)
                })

        if (!isRunning) {
            isRunning = true
            runningSubject.onNext(true)
        }
    }

    fun destroySubscriptions() {
        if (isRunning) {
            isRunning = false
            runningSubject.onNext(false)
        }

        if (downloadsSubscription != null) {
            downloadsSubscription?.unsubscribe()
            downloadsSubscription = null
        }

        if (threadsSubscription != null) {
            threadsSubscription?.unsubscribe()
        }

    }

    // Create a download object for every chapter and add them to the downloads queue
    fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        val source = sourceManager.get(manga.source)

        // Used to avoid downloading chapters with the same name
        val addedChapters = ArrayList<String>()
        val pending = ArrayList<Download>()

        for (chapter in chapters) {
            if (addedChapters.contains(chapter.name))
                continue

            addedChapters.add(chapter.name)
            val download = Download(source, manga, chapter)

            if (!prepareDownload(download)) {
                queue.add(download)
                pending.add(download)
            }
        }

        // Initialize queue size
        downloadNotifier.initialQueueSize = queue.size
        // Show notification
        downloadNotifier.onProgressChange(queue)

        if (isRunning) downloadsQueueSubject.onNext(pending)
    }

    // Public method to check if a chapter is downloaded
    fun isChapterDownloaded(source: Source, manga: Manga, chapter: Chapter): Boolean {
        val directory = getAbsoluteChapterDirectory(source, manga, chapter)
        if (!directory.exists())
            return false

        val pages = getSavedPageList(source, manga, chapter)
        return isChapterDownloaded(directory, pages)
    }

    // Prepare the download. Returns true if the chapter is already downloaded
    private fun prepareDownload(download: Download): Boolean {
        // If the chapter is already queued, don't add it again
        for (queuedDownload in queue) {
            if (download.chapter.id == queuedDownload.chapter.id)
                return true
        }

        // Add the directory to the download object for future access
        download.directory = getAbsoluteChapterDirectory(download)

        // If the directory doesn't exist, the chapter isn't downloaded.
        if (!download.directory.exists()) {
            return false
        }

        // If the page list doesn't exist, the chapter isn't downloaded
        val savedPages = getSavedPageList(download) ?: return false

        // Add the page list to the download object for future access
        download.pages = savedPages

        // If the number of files matches the number of pages, the chapter is downloaded.
        // We have the index file, so we check one file more
        return isChapterDownloaded(download.directory, download.pages)
    }

    // Check that all the images are downloaded
    private fun isChapterDownloaded(directory: File, pages: List<Page>?): Boolean {
        return pages != null && !pages.isEmpty() && pages.size + 1 == directory.listFiles().size
    }

    // Download the entire chapter
    private fun downloadChapter(download: Download): Observable<Download> {
        DiskUtils.createDirectory(download.directory)

        val pageListObservable = if (download.pages == null)
        // Pull page list from network and add them to download object
            download.source.pullPageListFromNetwork(download.chapter.url)
                    .doOnNext { pages ->
                        download.pages = pages
                        savePageList(download)
                    }
        else
        // Or if the page list already exists, start from the file
            Observable.just(download.pages)

        return Observable.defer {
            pageListObservable
                    .doOnNext { pages ->
                        download.downloadedImages = 0
                        download.status = Download.DOWNLOADING
                    }
                    // Get all the URLs to the source images, fetch pages if necessary
                    .flatMap { download.source.getAllImageUrlsFromPageList(it) }
                    // Start downloading images, consider we can have downloaded images already
                    .concatMap { page -> getOrDownloadImage(page, download) }
                    // Do when page is downloaded.
                    .doOnNext {
                        downloadNotifier.onProgressChange(download, queue)
                    }
                    // Do after download completes
                    .doOnCompleted { onDownloadCompleted(download) }
                    .toList()
                    .map { pages -> download }
                    // If the page list threw, it will resume here
                    .onErrorResumeNext { error ->
                        download.status = Download.ERROR
                        downloadNotifier.onError(error.message, download.chapter.name)
                        Observable.just(download)
                    }
        }.subscribeOn(Schedulers.io())
    }

    // Get the image from the filesystem if it exists or download from network
    private fun getOrDownloadImage(page: Page, download: Download): Observable<Page> {
        // If the image URL is empty, do nothing
        if (page.imageUrl == null)
            return Observable.just(page)

        val filename = getImageFilename(page)
        val imagePath = File(download.directory, filename)

        // If the image is already downloaded, do nothing. Otherwise download from network
        val pageObservable = if (isImageDownloaded(imagePath))
            Observable.just(page)
        else
            downloadImage(page, download.source, download.directory, filename)

        return pageObservable
                // When the image is ready, set image path, progress (just in case) and status
                .doOnNext {
                    page.imagePath = imagePath.absolutePath
                    page.progress = 100
                    download.downloadedImages++
                    page.status = Page.READY
                }
                // Mark this page as error and allow to download the remaining
                .onErrorResumeNext {
                    page.progress = 0
                    page.status = Page.ERROR
                    Observable.just(page)
                }
    }

    // Save image on disk
    private fun downloadImage(page: Page, source: Source, directory: File, filename: String): Observable<Page> {
        page.status = Page.DOWNLOAD_IMAGE
        return source.getImageProgressResponse(page)
                .flatMap {
                    try {
                        val file = File(directory, filename)
                        file.parentFile.mkdirs()
                        it.body().source().saveImageTo(file.outputStream(), preferences.reencodeImage())
                    } catch (e: Exception) {
                        it.body().close()
                        throw e
                    }
                    Observable.just(page)
                }
                .retryWhen {
                    it.zipWith(Observable.range(1, 3)) { errors, retries -> retries }
                            .flatMap { retries -> Observable.timer((retries * 2).toLong(), TimeUnit.SECONDS) }
                }
    }

    // Public method to get the image from the filesystem. It does NOT provide any way to download the image
    fun getDownloadedImage(page: Page, chapterDir: File): Observable<Page> {
        if (page.imageUrl == null) {
            page.status = Page.ERROR
            return Observable.just(page)
        }

        val imagePath = File(chapterDir, getImageFilename(page))

        // When the image is ready, set image path, progress (just in case) and status
        if (isImageDownloaded(imagePath)) {
            page.imagePath = imagePath.absolutePath
            page.progress = 100
            page.status = Page.READY
        } else {
            page.status = Page.ERROR
        }
        return Observable.just(page)
    }

    // Get the filename for an image given the page
    private fun getImageFilename(page: Page): String {
        val url = page.imageUrl
        val number = String.format("%03d", page.pageNumber + 1)

        // Try to preserve file extension
        return when {
            UrlUtil.isJpg(url) -> "$number.jpg"
            UrlUtil.isPng(url) -> "$number.png"
            UrlUtil.isGif(url) -> "$number.gif"
            else -> Uri.parse(url).lastPathSegment.replace(imageFilenameRegex, "_")
        }
    }

    private fun isImageDownloaded(imagePath: File): Boolean {
        return imagePath.exists()
    }

    // Called when a download finishes. This doesn't mean the download was successful, so we check it
    private fun onDownloadCompleted(download: Download) {
        checkDownloadIsSuccessful(download)
        savePageList(download)
    }

    private fun checkDownloadIsSuccessful(download: Download) {
        var actualProgress = 0
        var status = Download.DOWNLOADED
        // If any page has an error, the download result will be error
        for (page in download.pages) {
            actualProgress += page.progress
            if (page.status != Page.READY) {
                status = Download.ERROR
                downloadNotifier.onError(context.getString(R.string.download_notifier_page_ready_error), download.chapter.name)
            }
        }
        // Ensure that the chapter folder has all the images
        if (!isChapterDownloaded(download.directory, download.pages)) {
            status = Download.ERROR
            downloadNotifier.onError(context.getString(R.string.download_notifier_page_error), download.chapter.name)
        }
        download.totalProgress = actualProgress
        download.status = status
    }

    // Return the page list from the chapter's directory if it exists, null otherwise
    fun getSavedPageList(source: Source, manga: Manga, chapter: Chapter): List<Page>? {
        val chapterDir = getAbsoluteChapterDirectory(source, manga, chapter)
        val pagesFile = File(chapterDir, PAGE_LIST_FILE)

        return try {
            JsonReader(FileReader(pagesFile)).use {
                val collectionType = object : TypeToken<List<Page>>() {}.type
                gson.fromJson(it, collectionType)
            }
        } catch (e: Exception) {
            null
        }
    }

    // Shortcut for the method above
    private fun getSavedPageList(download: Download): List<Page>? {
        return getSavedPageList(download.source, download.manga, download.chapter)
    }

    // Save the page list to the chapter's directory
    fun savePageList(source: Source, manga: Manga, chapter: Chapter, pages: List<Page>) {
        val chapterDir = getAbsoluteChapterDirectory(source, manga, chapter)
        val pagesFile = File(chapterDir, PAGE_LIST_FILE)

        pagesFile.outputStream().use {
            try {
                it.write(gson.toJson(pages).toByteArray())
                it.flush()
            } catch (e: Exception) {
                Timber.e(e, e.message)
            }
        }
    }

    // Shortcut for the method above
    private fun savePageList(download: Download) {
        savePageList(download.source, download.manga, download.chapter, download.pages)
    }

    fun getAbsoluteMangaDirectory(source: Source, manga: Manga): File {
        val mangaRelativePath = source.visibleName +
                File.separator +
                manga.title.replace("[^\\sa-zA-Z0-9.-]".toRegex(), "_")

        return File(preferences.downloadsDirectory().getOrDefault(), mangaRelativePath)
    }

    // Get the absolute path to the chapter directory
    fun getAbsoluteChapterDirectory(source: Source, manga: Manga, chapter: Chapter): File {
        val chapterRelativePath = chapter.name.replace("[^\\sa-zA-Z0-9.-]".toRegex(), "_")

        return File(getAbsoluteMangaDirectory(source, manga), chapterRelativePath)
    }

    // Shortcut for the method above
    private fun getAbsoluteChapterDirectory(download: Download): File {
        return getAbsoluteChapterDirectory(download.source, download.manga, download.chapter)
    }

    fun deleteChapter(source: Source, manga: Manga, chapter: Chapter) {
        val path = getAbsoluteChapterDirectory(source, manga, chapter)
        DiskUtils.deleteFiles(path)
    }

    fun areAllDownloadsFinished(): Boolean {
        for (download in queue) {
            if (download.status <= Download.DOWNLOADING)
                return false
        }
        return true
    }

    fun startDownloads(): Boolean {
        if (queue.isEmpty())
            return false

        if (downloadsSubscription == null || downloadsSubscription!!.isUnsubscribed)
            initializeSubscriptions()

        val pending = ArrayList<Download>()
        for (download in queue) {
            if (download.status != Download.DOWNLOADED) {
                if (download.status != Download.QUEUE) download.status = Download.QUEUE
                pending.add(download)
            }
        }
        downloadsQueueSubject.onNext(pending)

        return !pending.isEmpty()
    }

    fun stopDownloads(error: String = "") {
        destroySubscriptions()
        for (download in queue) {
            if (download.status == Download.DOWNLOADING) {
                download.status = Download.ERROR
            }
        }
        downloadNotifier.onError(error)
    }

    fun clearQueue() {
        queue.clear()
        downloadNotifier.onClear()
    }

}
