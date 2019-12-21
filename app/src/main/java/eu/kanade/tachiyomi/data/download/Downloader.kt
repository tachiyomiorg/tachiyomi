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
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.fetchAllImageUrlsFromPageList
import eu.kanade.tachiyomi.util.*
import kotlinx.coroutines.experimental.async
import okhttp3.Response
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber

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
 * @param cache the downloads cache, used to add the downloads to the cache after their completion.
 * @param sourceManager the source manager.
 */
class Downloader(
        private val context: Context,
        private val provider: DownloadProvider,
        private val cache: DownloadCache,
        private val sourceManager: SourceManager
) {

    /**
     * Store for persisting downloads across restarts.
     */
    private val store = DownloadStore(context, sourceManager)

    /**
     * Queue where active downloads are kept.
     */
    val queue = DownloadQueue(store)

    /**
     * Notifier for the downloader state and progress.
     */
    private val notifier by lazy { DownloadNotifier(context) }

    /**
     * Downloader subscriptions.
     */
    private val subscriptions = CompositeSubscription()

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
        launchNow {
            val chapters = async { store.restore() }
            queue.addAll(chapters.await())
        }
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

        subscriptions += downloadsRelay.concatMapIterable { it }
                .concatMap { downloadChapter(it).subscribeOn(Schedulers.io()) }
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
     * Creates a download object for every chapter and adds them to the downloads queue.
     *
     * @param manga the manga of the chapters to download.
     * @param chapters the list of chapters to download.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    fun queueChapters(manga: Manga, chapters: List<Chapter>, autoStart: Boolean) = launchUI {
        val source = sourceManager.get(manga.source) as? HttpSource ?: return@launchUI

        // Called in background thread, the operation can be slow with SAF.
        val chaptersWithoutDir = async {
            val mangaDir = provider.findMangaDir(manga, source)

            chapters
                    // Avoid downloading chapters with the same name.
                    .distinctBy { it.name }
                    // Filter out those already downloaded.
                    .filter { mangaDir?.findFile(provider.getChapterDirName(it)) == null }
                    // Add chapters to queue from the start.
                    .sortedByDescending { it.source_order }
        }

        // Runs in main thread (synchronization needed).
        val chaptersToQueue = chaptersWithoutDir.await()
                // Filter out those already enqueued.
                .filter { chapter -> queue.none { it.chapter.id == chapter.id } }
                // Create a download for each one.
                .map { Download(source, manga, it) }

        if (chaptersToQueue.isNotEmpty()) {
            queue.addAll(chaptersToQueue)

            // Initialize queue size.
            notifier.initialQueueSize = queue.size

            if (isRunning) {
                // Send the list of downloads to the downloader.
                downloadsRelay.call(chaptersToQueue)
            }

            // Start downloader if needed
            if (autoStart) {
                DownloadService.start(this@Downloader.context)
            }
        }
    }

    /**
     * Returns the observable which downloads a chapter.
     *
     * @param download the chapter to be downloaded.
     */
    private fun downloadChapter(download: Download): Observable<Download> = Observable.defer {
        val chapterDirname = provider.getChapterDirName(download.chapter)
        val mangaDir = provider.getMangaDir(download.manga, download.source)
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

        pageListObservable
                .doOnNext { _ ->
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
                .doOnNext { notifier.onProgressChange(download) }
                .toList()
                .map { _ -> download }
                // Do after download completes
                .doOnNext { ensureSuccessfulDownload(download, mangaDir, tmpDir, chapterDirname) }
                // If the page list threw, it will resume here
                .onErrorReturn { error ->
                    download.status = Download.ERROR
                    notifier.onError(error.message, download.chapter.name)
                    download
                }

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
                        response.body()!!.source().saveTo(file.openOutputStream())
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
        val mime = response.body()?.contentType()?.let { ct -> "${ct.type()}/${ct.subtype()}" }
            // Else guess from the uri.
            ?: context.contentResolver.getType(file.uri)
            // Else read magic numbers.
            ?: ImageUtil.findImageType { file.openInputStream() }?.mime

        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
    }

    /**
     * Checks if the download was successful.
     *
     * @param download the download to check.
     * @param mangaDir the manga directory of the download.
     * @param tmpDir the directory where the download is currently stored.
     * @param dirname the real (non temporary) directory name of the download.
     */
    private fun ensureSuccessfulDownload(download: Download, mangaDir: UniFile,
                                         tmpDir: UniFile, dirname: String) {

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
            cache.addChapter(dirname, mangaDir, download.manga)
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
