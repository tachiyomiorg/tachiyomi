package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.webkit.MimeTypeMap
import com.hippo.unifile.UniFile
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.fetchAllImageUrlsFromPageList
import eu.kanade.tachiyomi.util.DynamicConcurrentMergeOperator
import eu.kanade.tachiyomi.util.RetryWithDelay
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.util.saveTo
import okhttp3.Response
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.net.URLConnection

/**
 * This class is the one in charge of downloading chapters.
 *
 * Its [queue] contains the list of chapters to download. In order to download them, the downloader
 * subscriptions must be running and the list of chapters must be sent to them by [downloadsRelay].
 *
 * The queue manipulation must be done in one thread (currently the main thread) to avoid unexpected
 * behavior, but it's safe to read it from multiple threads.
 *
 * @param context the application context.
 * @param provider the downloads directory provider.
 */
class Downloader(private val context: Context, private val provider: DownloadProvider) {

    /**
     * Store for persisting downloads across restarts.
     */
    private val store = DownloadStore(context)

    /**
     * Queue where active downloads are kept.
     */
    val queue = DownloadQueue(store)

    /**
     * Source manager.
     */
    private val sourceManager: SourceManager by injectLazy()

    /**
     * Preferences.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Notifier for the downloader state and progress.
     */
    private val notifier by lazy { DownloadNotifier(context) }

    /**
     * Downloader subscriptions.
     */
    private val subscriptions = CompositeSubscription()

    /**
     * Subject to do a live update of the number of simultaneous downloads.
     */
    private val threadsSubject = BehaviorSubject.create<Int>()

    /**
     * Relay to send a list of downloads to the downloader.
     */
    private val downloadsRelay = PublishRelay.create<List<Download>>()

    /**
     * Relay to subscribe to the downloader status.
     */
    val runningRelay: BehaviorRelay<Boolean> = BehaviorRelay.create(false)

    /**
     * Whether the downloader is running.
     */
    @Volatile private var isRunning: Boolean = false

    init {
        Observable.fromCallable { store.restore() }
                .map { downloads -> downloads.filter { isDownloadAllowed(it) } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ downloads -> queue.addAll(downloads)
                }, { error -> Timber.e(error) })
    }

    /**
     * Starts the downloader. It doesn't do anything if it's already running or there isn't anything
     * to download.
     *
     * @return true if the downloader is started, false otherwise.
     */
    fun start(): Boolean {
        if (isRunning || queue.isEmpty())
            return false

        if (!subscriptions.hasSubscriptions())
            initializeSubscriptions()

        val pending = queue.filter { it.status != Download.DOWNLOADED }
        pending.forEach { if (it.status != Download.QUEUE) it.status = Download.QUEUE }

        downloadsRelay.call(pending)
        return !pending.isEmpty()
    }

    /**
     * Stops the downloader.
     */
    fun stop(reason: String? = null) {
        destroySubscriptions()
        queue
                .filter { it.status == Download.DOWNLOADING }
                .forEach { it.status = Download.ERROR }

        if (reason != null) {
            notifier.onWarning(reason)
        } else {
            if (notifier.paused) {
                notifier.paused = false
                notifier.onDownloadPaused()
            } else if (notifier.isSingleChapter && !notifier.errorThrown) {
                notifier.isSingleChapter = false
            } else {
                notifier.dismiss()
            }
        }
    }

    /**
     * Pauses the downloader
     */
    fun pause() {
        destroySubscriptions()
        queue
                .filter { it.status == Download.DOWNLOADING }
                .forEach { it.status = Download.QUEUE }
        notifier.paused = true
    }

    /**
     * Removes everything from the queue.
     *
     * @param isNotification value that determines if status is set (needed for view updates)
     */
    fun clearQueue(isNotification: Boolean = false) {
        destroySubscriptions()

        //Needed to update the chapter view
        if (isNotification) {
            queue
                    .filter { it.status == Download.QUEUE }
                    .forEach { it.status = Download.NOT_DOWNLOADED }
        }
        queue.clear()
        notifier.dismiss()
    }

    /**
     * Prepares the subscriptions to start downloading.
     */
    private fun initializeSubscriptions() {
        if (isRunning) return
        isRunning = true
        runningRelay.call(true)

        subscriptions.clear()

        subscriptions += preferences.downloadThreads().asObservable()
                .subscribe {
                    threadsSubject.onNext(it)
                    notifier.multipleDownloadThreads = it > 1
                }

        subscriptions += downloadsRelay.flatMap { Observable.from(it) }
                .lift(DynamicConcurrentMergeOperator<Download, Download>({ downloadChapter(it) }, threadsSubject))
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ completeDownload(it)
                }, { error ->
                    DownloadService.stop(context)
                    Timber.e(error)
                    notifier.onError(error.message)
                })
    }

    /**
     * Destroys the downloader subscriptions.
     */
    private fun destroySubscriptions() {
        if (!isRunning) return
        isRunning = false
        runningRelay.call(false)

        subscriptions.clear()
    }

    /**
     * Creates a download object for every chapter and adds them to the downloads queue. This method
     * must be called in the main thread.
     *
     * @param manga the manga of the chapters to download.
     * @param chapters the list of chapters to download.
     */
    fun queueChapters(manga: Manga, chapters: List<Chapter>) {
        val source = sourceManager.get(manga.source) as? HttpSource ?: return

        val chaptersToQueue = chapters
                // Avoid downloading chapters with the same name.
                .distinctBy { it.name }
                // Add chapters to queue from the start.
                .sortedByDescending { it.source_order }
                // Create a downloader for each one.
                .map { Download(source, manga, it) }
                // Filter out those already queued or downloaded.
                .filter { isDownloadAllowed(it) }

        // Return if there's nothing to queue.
        if (chaptersToQueue.isEmpty())
            return

        queue.addAll(chaptersToQueue)

        // Initialize queue size.
        notifier.initialQueueSize = queue.size

        if (isRunning) {
            // Send the list of downloads to the downloader.
            downloadsRelay.call(chaptersToQueue)
        } else {
            // Show initial notification.
            notifier.onProgressChange(queue)
        }
    }

    /**
     * Returns true if the given download can be queued and downloaded.
     *
     * @param download the download to be checked.
     */
    private fun isDownloadAllowed(download: Download): Boolean {
        // If the chapter is already queued, don't add it again
        if (queue.any { it.chapter.id == download.chapter.id })
            return false

        val dir = provider.findChapterDir(download.source, download.manga, download.chapter)
        if (dir != null && dir.exists())
            return false

        return true
    }

    /**
     * Returns the observable which downloads a chapter.
     *
     * @param download the chapter to be downloaded.
     */
    private fun downloadChapter(download: Download): Observable<Download> {
        val chapterDirname = provider.getChapterDirName(download.chapter)
        val mangaDir = provider.getMangaDir(download.source, download.manga)
        val tmpDir = mangaDir.createDirectory("${chapterDirname}_tmp")

        val pageListObservable = if (download.pages == null) {
            // Pull page list from network and add them to download object
            download.source.fetchPageList(download.chapter)
                    .doOnNext { pages ->
                        if (pages.isEmpty()) {
                            throw Exception("Page list is empty")
                        }
                        download.pages = pages
                    }
        } else {
            // Or if the page list already exists, start from the file
            Observable.just(download.pages!!)
        }

        return pageListObservable
                .doOnNext { pages ->
                    // Delete all temporary (unfinished) files
                    tmpDir.listFiles()
                            ?.filter { it.name!!.endsWith(".tmp") }
                            ?.forEach { it.delete() }

                    download.downloadedImages = 0
                    download.status = Download.DOWNLOADING
                }
                // Get all the URLs to the source images, fetch pages if necessary
                .flatMap { download.source.fetchAllImageUrlsFromPageList(it) }
                // Start downloading images, consider we can have downloaded images already
                .concatMap { page -> getOrDownloadImage(page, download, tmpDir) }
                // Do when page is downloaded.
                .doOnNext { notifier.onProgressChange(download, queue) }
                .toList()
                .map { pages -> download }
                // Do after download completes
                .doOnNext { ensureSuccessfulDownload(download, tmpDir, chapterDirname) }
                // If the page list threw, it will resume here
                .onErrorReturn { error ->
                    download.status = Download.ERROR
                    notifier.onError(error.message, download.chapter.name)
                    download
                }
                .subscribeOn(Schedulers.io())
    }

    /**
     * Returns the observable which gets the image from the filesystem if it exists or downloads it
     * otherwise.
     *
     * @param page the page to download.
     * @param download the download of the page.
     * @param tmpDir the temporary directory of the download.
     */
    private fun getOrDownloadImage(page: Page, download: Download, tmpDir: UniFile): Observable<Page> {
        // If the image URL is empty, do nothing
        if (page.imageUrl == null)
            return Observable.just(page)

        val filename = String.format("%03d", page.number)
        val tmpFile = tmpDir.findFile("$filename.tmp")

        // Delete temp file if it exists.
        tmpFile?.delete()

        // Try to find the image file.
        val imageFile = tmpDir.listFiles()!!.find { it.name!!.startsWith("$filename.") }

        // If the image is already downloaded, do nothing. Otherwise download from network
        val pageObservable = if (imageFile != null)
            Observable.just(imageFile)
        else
            downloadImage(page, download.source, tmpDir, filename)

        return pageObservable
                // When the image is ready, set image path, progress (just in case) and status
                .doOnNext { file ->
                    page.uri = file.uri
                    page.progress = 100
                    download.downloadedImages++
                    page.status = Page.READY
                }
                .map { page }
                // Mark this page as error and allow to download the remaining
                .onErrorReturn {
                    page.progress = 0
                    page.status = Page.ERROR
                    page
                }
    }

    /**
     * Returns the observable which downloads the image from network.
     *
     * @param page the page to download.
     * @param source the source of the page.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the image.
     */
    private fun downloadImage(page: Page, source: HttpSource, tmpDir: UniFile, filename: String): Observable<UniFile> {
        page.status = Page.DOWNLOAD_IMAGE
        page.progress = 0
        return source.fetchImage(page)
                .map { response ->
                    val file = tmpDir.createFile("$filename.tmp")
                    try {
                        response.body().source().saveTo(file.openOutputStream())
                        val extension = getImageExtension(response, file)
                        file.renameTo("$filename.$extension")
                    } catch (e: Exception) {
                        response.close()
                        file.delete()
                        throw e
                    }
                    file
                }
                // Retry 3 times, waiting 2, 4 and 8 seconds between attempts.
                .retryWhen(RetryWithDelay(3, { (2 shl it - 1) * 1000 }, Schedulers.trampoline()))
    }

    /**
     * Returns the extension of the downloaded image from the network response, or if it's null,
     * analyze the file. If everything fails, assume it's a jpg.
     *
     * @param response the network response of the image.
     * @param file the file where the image is already downloaded.
     */
    private fun getImageExtension(response: Response, file: UniFile): String {
        // Read content type if available.
        val mime = response.body().contentType()?.let { ct -> "${ct.type()}/${ct.subtype()}" }
            // Else guess from the uri.
            ?: context.contentResolver.getType(file.uri)
            // Else read magic numbers.
            ?: file.openInputStream().buffered().use {
            URLConnection.guessContentTypeFromStream(it)
        }

        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
    }

    /**
     * Checks if the download was successful.
     *
     * @param download the download to check.
     * @param tmpDir the directory where the download is currently stored.
     * @param dirname the real (non temporary) directory name of the download.
     */
    private fun ensureSuccessfulDownload(download: Download, tmpDir: UniFile, dirname: String) {
        // Ensure that the chapter folder has all the images.
        val downloadedImages = tmpDir.listFiles().orEmpty().filterNot { it.name!!.endsWith(".tmp") }

        download.status = if (downloadedImages.size == download.pages!!.size) {
            Download.DOWNLOADED
        } else {
            Download.ERROR
        }

        // Only rename the directory if it's downloaded.
        if (download.status == Download.DOWNLOADED) {
            tmpDir.renameTo(dirname)
        }
    }

    /**
     * Completes a download. This method is called in the main thread.
     */
    private fun completeDownload(download: Download) {
        // Delete successful downloads from queue
        if (download.status == Download.DOWNLOADED) {
            // remove downloaded chapter from queue
            queue.remove(download)
            notifier.onProgressChange(queue)
        }
        if (areAllDownloadsFinished()) {
            if (notifier.isSingleChapter && !notifier.errorThrown) {
                notifier.onDownloadCompleted(download, queue)
            }
            DownloadService.stop(context)
        }
    }

    /**
     * Returns true if all the queued downloads are in DOWNLOADED or ERROR state.
     */
    private fun areAllDownloadsFinished(): Boolean {
        return queue.none { it.status <= Download.DOWNLOADING }
    }

}