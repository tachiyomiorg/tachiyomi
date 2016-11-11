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
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.util.DynamicConcurrentMergeOperator
import eu.kanade.tachiyomi.util.RetryWithDelay
import eu.kanade.tachiyomi.util.isNullOrUnsubscribed
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

class Downloader(val context: Context, val provider: DownloadProvider) {

    private val store = DownloadStore(context)

    val queue = DownloadQueue(store)

    private val sourceManager: SourceManager = Injekt.get()
    private val preferences: PreferencesHelper = Injekt.get()

    private val downloadsQueueSubject = PublishSubject.create<List<Download>>()

    private var downloadsSubscription: Subscription? = null

    private val notifier by lazy { DownloadNotifier(context) }
    val runningSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    private val threadsSubject = BehaviorSubject.create<Int>()

    private var threadsSubscription: Subscription? = null

    @Volatile var isRunning: Boolean = false
        private set

    init {
        Observable.fromCallable { store.restore() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ downloads -> downloads.forEach {
                    if (!prepareDownload(it)) {
                        queue.add(it)
                    }
                }}, { error -> Timber.e(error) })
    }

    fun start(): Boolean {
        if (queue.isEmpty())
            return false

        if (downloadsSubscription.isNullOrUnsubscribed())
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

    fun stop(errorMessage: String? = null) {
        destroySubscriptions()
        for (download in queue) {
            if (download.status == Download.DOWNLOADING) {
                download.status = Download.ERROR
            }
        }
        errorMessage?.let { notifier.onError(it) }
    }

    fun clearQueue() {
        destroySubscriptions()
        queue.clear()
        notifier.onClear()
    }

    private fun initializeSubscriptions() {
        if (isRunning) return
        isRunning = true

        threadsSubscription?.unsubscribe()
        threadsSubscription = preferences.downloadThreads().asObservable()
                .subscribe {
                    threadsSubject.onNext(it)
                    notifier.multipleDownloadThreads = it > 1
                }

        downloadsSubscription?.unsubscribe()
        downloadsSubscription = downloadsQueueSubject.flatMap { Observable.from(it) }
                .lift(DynamicConcurrentMergeOperator<Download, Download>({ downloadChapter(it) }, threadsSubject))
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Delete successful downloads from queue
                    if (it.status == Download.DOWNLOADED) {
                        // remove downloaded chapter from queue
                        queue.del(it)
                        notifier.onProgressChange(queue)
                    }
                    if (areAllDownloadsFinished()) {
                        DownloadService.stop(context)
                    }
                }, { error ->
                    DownloadService.stop(context)
                    Timber.e(error)
                    notifier.onError(error.message)
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
    fun queueChapters(manga: Manga, chapters: List<Chapter>) {
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
        notifier.initialQueueSize = queue.size
        // Show notification
        notifier.onProgressChange(queue)

        if (isRunning) downloadsQueueSubject.onNext(pending)
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
        return false
    }

    // Download the entire chapter
    private fun downloadChapter(download: Download): Observable<Download> {
        val dirname = provider.getChapterDirName(download.chapter)
        val tmpDir = download.directory.parentFile!!.subFile("${dirname}_tmp")!!

        val pageListObservable: Observable<List<Page>> = if (download.pages == null)
        // Pull page list from network and add them to download object
            download.source.fetchPageListFromNetwork(download.chapter)
                    .doOnNext { pages ->
                        download.pages = pages
                    }
        else
        // Or if the page list already exists, start from the file
            Observable.just(download.pages)

        return pageListObservable
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
                    notifier.onProgressChange(download, queue)
                }
                // Do after download completes
                .doOnCompleted { onDownloadCompleted(download, tmpDir, dirname) }
                .toList()
                .map { pages -> download }
                // If the page list threw, it will resume here
                .onErrorReturn { error ->
                    download.status = Download.ERROR
                    notifier.onError(error.message, download.chapter.name)
                    download
                }
                .subscribeOn(Schedulers.io())
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

    // Called when a download finishes. This doesn't mean the download was successful, so we check it
    private fun onDownloadCompleted(download: Download, tmpDir: UniFile, dirname: String) {
        var actualProgress = 0
        var status = Download.DOWNLOADED

        // If any page has an error, the download result will be error
        for (page in download.pages!!) {
            actualProgress += page.progress
            if (page.status != Page.READY) {
                status = Download.ERROR
                notifier.onError(context.getString(R.string.download_notifier_page_ready_error), download.chapter.name)
            }
        }
        // Ensure that the chapter folder has all the images
        val downloadedImages = tmpDir.listFiles()!!.filterNot { it.name!!.endsWith(".tmp") }
        if (downloadedImages.size < download.pages!!.size) {
            status = Download.ERROR
            notifier.onError(context.getString(R.string.download_notifier_page_error), download.chapter.name)
        }

        if (status == Download.DOWNLOADED) {
            tmpDir.renameTo(dirname)
        }

        download.totalProgress = actualProgress
        download.status = status
    }

    fun areAllDownloadsFinished(): Boolean {
        for (download in queue) {
            if (download.status <= Download.DOWNLOADING)
                return false
        }
        return true
    }

}