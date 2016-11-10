package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.webkit.MimeTypeMap
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.util.DynamicConcurrentMergeOperator
import eu.kanade.tachiyomi.util.RetryWithDelay
import eu.kanade.tachiyomi.util.saveTo
import okhttp3.Response
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.*

class DownloadManager(
        private val context: Context,
        private val sourceManager: SourceManager = Injekt.get(),
        private val preferences: PreferencesHelper = Injekt.get()
) {

    private val provider = DownloadProvider(context)

    private val downloadsQueueSubject = PublishSubject.create<List<Download>>()
    val runningSubject = BehaviorSubject.create<Boolean>()
    private var downloadsSubscription: Subscription? = null

    val downloadNotifier by lazy { DownloadNotifier(context) }

    private val threadsSubject = BehaviorSubject.create<Int>()
    private var threadsSubscription: Subscription? = null

    val queue = DownloadQueue()

    @Volatile var isRunning: Boolean = false
        private set

    private fun initializeSubscriptions() {
        if (isRunning) return
        isRunning = true

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
                }, { error ->
                    DownloadService.stop(context)
                    Timber.e(error)
                    downloadNotifier.onError(error.message)
                })

        runningSubject.onNext(true)
    }

    private fun destroySubscriptions() {
        if (!isRunning) return
        isRunning = false
        runningSubject.onNext(false)

        downloadsSubscription?.unsubscribe()
        downloadsSubscription = null

        threadsSubscription?.unsubscribe()
        threadsSubscription = null
    }

    // Create a download object for every chapter and add them to the downloads queue
    fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        val source = sourceManager.get(manga.source) as? OnlineSource ?: return

        // Add chapters to queue from the start
        val sortedChapters = chapters.sortedByDescending { it.source_order }

        // Used to avoid downloading chapters with the same name
        val addedChapters = ArrayList<String>()
        val pending = ArrayList<Download>()

        for (chapter in sortedChapters) {
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
        val mangaDir = provider.getMangaDir(source, manga)
        return provider.getChapterDir(mangaDir, chapter).exists()
    }

    // Prepare the download. Returns true if the chapter is already downloaded
    private fun prepareDownload(download: Download): Boolean {
        // If the chapter is already queued, don't add it again
        for (queuedDownload in queue) {
            if (download.chapter.id == queuedDownload.chapter.id)
                return true
        }

        val mangaDir = provider.getMangaDir(download.source, download.manga)
        val filename = provider.getChapterDirName(download.chapter)
        val chapterDir = mangaDir.subFile(filename)!!

        if (chapterDir.exists())
            return true

        download.directory = chapterDir
        download.filename = filename
        return false
    }

    // Download the entire chapter
    private fun downloadChapter(download: Download): Observable<Download> {
        val tmpDir = download.directory.parentFile!!.subFile("${download.filename}_tmp")!!

        val pageListObservable: Observable<List<Page>> = if (download.pages == null)
            // Pull page list from network and add them to download object
            download.source.fetchPageListFromNetwork(download.chapter)
                    .doOnNext { pages ->
                        download.pages = pages
                    }
        else
            // Or if the page list already exists, start from the file
            Observable.just(download.pages)

        return Observable.defer {
            pageListObservable
                    .doOnNext { pages ->
                        tmpDir.ensureDir()

                        // Delete all temporary files
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
                    .doOnNext {
                        downloadNotifier.onProgressChange(download, queue)
                    }
                    // Do after download completes
                    .doOnCompleted { onDownloadCompleted(download, tmpDir) }
                    .toList()
                    .map { pages -> download }
                    // If the page list threw, it will resume here
                    .onErrorReturn { error ->
                        download.status = Download.ERROR
                        downloadNotifier.onError(error.message, download.chapter.name)
                        download
                    }
        }.subscribeOn(Schedulers.io())
    }

    // Get the image from the filesystem if it exists or download from network
    private fun getOrDownloadImage(page: Page, download: Download, tmpDir: UniFile): Observable<Page> {
        // If the image URL is empty, do nothing
        if (page.imageUrl == null)
            return Observable.just(page)

        val filename = String.format("%03d", page.pageNumber + 1)
        val tmpFile = tmpDir.findFile("$filename.tmp")

        // Delete temp file if it exists.
        tmpFile?.delete()

        // Try to find the image file.
        val imageFile = tmpDir.listFiles()!!.find { it.name!!.startsWith("$filename.")}

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

    // Save image on disk
    private fun downloadImage(page: Page, source: OnlineSource, tmpDir: UniFile, filename: String): Observable<UniFile> {
        page.status = Page.DOWNLOAD_IMAGE
        page.progress = 0
        return source.imageResponse(page)
                .map { response ->
                    val file = tmpDir.createFile("$filename.tmp")
                    try {
                        response.body().source().saveTo(file.openOutputStream())
                        val extension = getFileExtension(response, file)
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

    private fun getFileExtension(response: Response, file: UniFile): String? {
        val contentType = response.body().contentType()
        val mimeStr = if (contentType != null) {
            "${contentType.type()}/${contentType.subtype()}"
        } else {
            context.contentResolver.getType(file.uri)
        }
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeStr)
    }

    fun buildPageList(chapterDir: UniFile): Observable<List<Page>> {
        return Observable.fromCallable {
            val pages = mutableListOf<Page>()
            chapterDir.listFiles()
                    ?.filter { it.type?.startsWith("image") ?: false }
                    ?.forEach { file ->
                        val page = Page(pages.size, uri = file.uri)
                        pages.add(page)
                        page.status = Page.READY
                    }
            pages
        }
    }

    fun buildPageList(source: Source, manga: Manga, chapter: Chapter): Observable<List<Page>> {
        val mangaDir = provider.getMangaDir(source, manga)
        return buildPageList(provider.getChapterDir(mangaDir, chapter))
    }

    // Called when a download finishes. This doesn't mean the download was successful, so we check it
    private fun onDownloadCompleted(download: Download, tmpDir: UniFile) {
        var actualProgress = 0
        var status = Download.DOWNLOADED

        // If any page has an error, the download result will be error
        for (page in download.pages!!) {
            actualProgress += page.progress
            if (page.status != Page.READY) {
                status = Download.ERROR
                downloadNotifier.onError(context.getString(R.string.download_notifier_page_ready_error), download.chapter.name)
            }
        }
        // Ensure that the chapter folder has all the images
        val downloadedImages = tmpDir.listFiles()!!.filterNot { it.name!!.endsWith(".tmp") }
        if (downloadedImages.size < download.pages!!.size) {
            status = Download.ERROR
            downloadNotifier.onError(context.getString(R.string.download_notifier_page_error), download.chapter.name)
        }

        if (status == Download.DOWNLOADED) {
            tmpDir.renameTo(download.filename)
        }

        download.totalProgress = actualProgress
        download.status = status
    }

    fun getMangaDir(source: Source, manga: Manga): UniFile {
        return provider.getMangaDir(source, manga)
    }

    fun getChapterDir(source: Source, manga: Manga, chapter: Chapter): UniFile {
        val mangaDir = provider.getMangaDir(source, manga)
        return provider.getChapterDir(mangaDir, chapter)
    }

    fun deleteChapter(source: Source, manga: Manga, chapter: Chapter) {
        getChapterDir(source, manga, chapter).delete()
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

    fun stopDownloads(errorMessage: String? = null) {
        destroySubscriptions()
        for (download in queue) {
            if (download.status == Download.DOWNLOADING) {
                download.status = Download.ERROR
            }
        }
        errorMessage?.let { downloadNotifier.onError(it) }
    }

    fun clearQueue() {
        queue.clear()
        downloadNotifier.onClear()
    }

}
