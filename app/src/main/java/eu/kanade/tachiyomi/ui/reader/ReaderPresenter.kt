package eu.kanade.tachiyomi.ui.reader

import android.os.Bundle
import android.os.Environment
import android.webkit.MimeTypeMap
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackUpdateService
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.reader.notification.ImageNotifier
import eu.kanade.tachiyomi.util.DiskUtil
import eu.kanade.tachiyomi.util.RetryWithDelay
import eu.kanade.tachiyomi.util.SharedData
import eu.kanade.tachiyomi.util.toast
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.net.URLConnection
import java.util.*

/**
 * Presenter of [ReaderActivity].
 */
class ReaderPresenter : BasePresenter<ReaderActivity>() {
    /**
     * Preferences.
     */
    val prefs: PreferencesHelper by injectLazy()

    /**
     * Database.
     */
    val db: DatabaseHelper by injectLazy()

    /**
     * Download manager.
     */
    val downloadManager: DownloadManager by injectLazy()

    /**
     * Tracking manager.
     */
    val trackManager: TrackManager by injectLazy()

    /**
     * Source manager.
     */
    val sourceManager: SourceManager by injectLazy()

    /**
     * Chapter cache.
     */
    val chapterCache: ChapterCache by injectLazy()

    /**
     * Cover cache.
     */
    val coverCache: CoverCache by injectLazy()

    /**
     * Manga being read.
     */
    lateinit var manga: Manga
        private set

    /**
     * Active chapter.
     */
    lateinit var chapter: ReaderChapter
        private set

    /**
     * Previous chapter of the active.
     */
    private var prevChapter: ReaderChapter? = null

    /**
     * Next chapter of the active.
     */
    private var nextChapter: ReaderChapter? = null

    /**
     * Source of the manga.
     */
    private val source by lazy { sourceManager.get(manga.source)!! }

    /**
     * Chapter list for the active manga. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val chapterList by lazy {
        val dbChapters = db.getChapters(manga).executeAsBlocking().map { it.toModel() }

        val sortFunction: (Chapter, Chapter) -> Int = when (manga.sorting) {
            Manga.SORTING_SOURCE -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            Manga.SORTING_NUMBER -> { c1, c2 -> c1.chapter_number.compareTo(c2.chapter_number) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        dbChapters.sortedWith(Comparator<Chapter> { c1, c2 -> sortFunction(c1, c2) })
    }

    /**
     * Map of chapters that have been loaded in the reader.
     */
    private val loadedChapters = hashMapOf<Long?, ReaderChapter>()

    /**
     * List of manga services linked to the active manga, or null if auto syncing is not enabled.
     */
    private var trackList: List<Track>? = null

    /**
     * Chapter loader whose job is to obtain the chapter list and initialize every page.
     */
    private val loader by lazy { ChapterLoader(downloadManager, manga, source) }

    /**
     * Subscription for appending a chapter to the reader (seamless mode).
     */
    private var appenderSubscription: Subscription? = null

    /**
     * Subscription for retrieving the adjacent chapters to the current one.
     */
    private var adjacentChaptersSubscription: Subscription? = null

    companion object {
        /**
         * Id of the restartable that loads the active chapter.
         */
        private const val LOAD_ACTIVE_CHAPTER = 1
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (savedState == null) {
            val event = SharedData.get(ReaderEvent::class.java) ?: return
            manga = event.manga
            chapter = event.chapter.toModel()
        } else {
            manga = savedState.getSerializable(ReaderPresenter::manga.name) as Manga
            chapter = savedState.getSerializable(ReaderPresenter::chapter.name) as ReaderChapter
        }

        // Send the active manga to the view to initialize the reader.
        Observable.just(manga)
                .subscribeLatestCache({ view, manga -> view.onMangaOpen(manga) })

        // Retrieve the sync list if auto syncing is enabled.
        if (prefs.autoUpdateTrack()) {
            add(db.getTracks(manga).asRxSingle()
                    .subscribe({ trackList = it }))
        }

        restartableLatestCache(LOAD_ACTIVE_CHAPTER,
                { loadChapterObservable(chapter) },
                { view, chapter -> view.onChapterReady(this.chapter) },
                { view, error -> view.onChapterError(error) })

        if (savedState == null) {
            loadChapter(chapter)
        }
    }

    override fun onSave(state: Bundle) {
        chapter.requestedPage = chapter.last_page_read
        state.putSerializable(ReaderPresenter::manga.name, manga)
        state.putSerializable(ReaderPresenter::chapter.name, chapter)
        super.onSave(state)
    }

    override fun onDestroy() {
        loader.cleanup()
        onChapterLeft()
        super.onDestroy()
    }

    /**
     * Converts a chapter to a [ReaderChapter] if needed.
     */
    private fun Chapter.toModel(): ReaderChapter {
        if (this is ReaderChapter) return this
        return ReaderChapter(this)
    }

    /**
     * Returns an observable that loads the given chapter, discarding any previous work.
     *
     * @param chapter the now active chapter.
     */
    private fun loadChapterObservable(chapter: ReaderChapter): Observable<ReaderChapter> {
        loader.restart()
        return loader.loadChapter(chapter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Obtains the adjacent chapters of the given one in a background thread, and notifies the view
     * when they are known.
     *
     * @param chapter the current active chapter.
     */
    private fun getAdjacentChapters(chapter: ReaderChapter) {
        // Keep only one subscription
        adjacentChaptersSubscription?.let { remove(it) }

        adjacentChaptersSubscription = Observable
                .fromCallable { getAdjacentChaptersStrategy(chapter) }
                .doOnNext { pair ->
                    prevChapter = loadedChapters.getOrElse(pair.first?.id) { pair.first }
                    nextChapter = loadedChapters.getOrElse(pair.second?.id) { pair.second }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache({ view, pair ->
                    view.onAdjacentChapters(pair.first, pair.second)
                })
    }

    /**
     * Returns the previous and next chapters of the given one in a [Pair] according to the sorting
     * strategy set for the manga.
     *
     * @param chapter the current active chapter.
     * @param previousChapterAmount the desired number of chapters preceding the current active chapter (Default: 1).
     * @param nextChapterAmount the desired number of chapters succeeding the current active chapter (Default: 1).
     */
    private fun getAdjacentChaptersStrategy(chapter: ReaderChapter, previousChapterAmount: Int = 1, nextChapterAmount: Int = 1) = when (manga.sorting) {
        Manga.SORTING_SOURCE -> {
            val currChapterIndex = chapterList.indexOfFirst { chapter.id == it.id }
            val nextChapter = chapterList.getOrNull(currChapterIndex + nextChapterAmount)
            val prevChapter = chapterList.getOrNull(currChapterIndex - previousChapterAmount)
            Pair(prevChapter, nextChapter)
        }
        Manga.SORTING_NUMBER -> {
            val currChapterIndex = chapterList.indexOfFirst { chapter.id == it.id }
            val chapterNumber = chapter.chapter_number

            var prevChapter: ReaderChapter? = null
            for (i in (currChapterIndex - previousChapterAmount) downTo 0) {
                val c = chapterList[i]
                if (c.chapter_number < chapterNumber && c.chapter_number >= chapterNumber - previousChapterAmount) {
                    prevChapter = c
                    break
                }
            }

            var nextChapter: ReaderChapter? = null
            for (i in (currChapterIndex + nextChapterAmount) until chapterList.size) {
                val c = chapterList[i]
                if (c.chapter_number > chapterNumber && c.chapter_number <= chapterNumber + nextChapterAmount) {
                    nextChapter = c
                    break
                }
            }
            Pair(prevChapter, nextChapter)
        }
        else -> throw NotImplementedError("Unknown sorting method")
    }

    /**
     * Loads the given chapter and sets it as the active one. This method also accepts a requested
     * page, which will be set as active when it's displayed in the view.
     *
     * @param chapter the chapter to load.
     * @param requestedPage the requested page from the view.
     */
    private fun loadChapter(chapter: ReaderChapter, requestedPage: Int = 0) {
        // Cleanup any append.
        appenderSubscription?.let { remove(it) }

        this.chapter = loadedChapters.getOrPut(chapter.id) { chapter }

        // If the chapter is partially read, set the starting page to the last the user read
        // otherwise use the requested page.
        chapter.requestedPage = if (!chapter.read) chapter.last_page_read else requestedPage

        // Reset next and previous chapter. They have to be fetched again
        nextChapter = null
        prevChapter = null

        start(LOAD_ACTIVE_CHAPTER)
        getAdjacentChapters(chapter)
    }

    /**
     * Changes the active chapter, but doesn't load anything. Called when changing chapters from
     * the reader with the seamless mode.
     *
     * @param chapter the chapter to set as active.
     */
    fun setActiveChapter(chapter: ReaderChapter) {
        onChapterLeft()
        this.chapter = chapter
        nextChapter = null
        prevChapter = null
        getAdjacentChapters(chapter)
    }

    /**
     * Appends the next chapter to the reader, if possible.
     */
    fun appendNextChapter() {
        appenderSubscription?.let { remove(it) }

        val nextChapter = nextChapter ?: return
        val chapterToLoad = loadedChapters.getOrPut(nextChapter.id) { nextChapter }

        appenderSubscription = loader.loadChapter(chapterToLoad)
                .subscribeOn(Schedulers.io())
                .retryWhen(RetryWithDelay(1, { 3000 }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache({ view, chapter ->
                    view.onAppendChapter(chapter)
                }, { view, error ->
                    view.onChapterAppendError()
                })
    }

    /**
     * Retries a page that failed to load due to network error or corruption.
     *
     * @param page the page that failed.
     */
    fun retryPage(page: Page?) {
        if (page != null && source is OnlineSource) {
            page.status = Page.QUEUE
            val uri = page.uri
            if (uri != null && !page.chapter.isDownloaded) {
                chapterCache.removeFileFromCache(uri.encodedPath.substringAfterLast('/'))
            }
            loader.retryPage(page)
        }
    }

    /**
     * Called before loading another chapter or leaving the reader. It allows to do operations
     * over the chapter read like saving progress
     */
    fun onChapterLeft() {
        // Reference these locally because they are needed later from another thread.
        val chapter = chapter

        val pages = chapter.pages ?: return

        Observable.fromCallable {
            // Cache current page list progress for online chapters to allow a faster reopen
            if (!chapter.isDownloaded) {
                source.let { if (it is OnlineSource) it.savePageList(chapter, pages) }
            }

            try {
                if (chapter.read) {
                    val removeAfterReadSlots = prefs.removeAfterReadSlots()
                    when (removeAfterReadSlots) {
                        // Setting disabled
                        -1 -> { /* Empty function */ }
                        // Remove current read chapter
                        0 -> deleteChapter(chapter, manga)
                        // Remove previous chapter specified by user in settings.
                        else -> getAdjacentChaptersStrategy(chapter, removeAfterReadSlots)
                                .first?.let { deleteChapter(it, manga) }
                    }
                }
            } catch (error: Exception) {
                // TODO find out why it crashes
                Timber.e(error)
            }

            db.updateChapterProgress(chapter).executeAsBlocking()

            try {
                val history = History.create(chapter).apply { last_read = Date().time }
                db.updateHistoryLastRead(history).executeAsBlocking()
            } catch (error: Exception) {
                // TODO find out why it crashes
                Timber.e(error)
            }
        }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    /**
     * Called when the active page changes in the reader.
     *
     * @param page the active page
     */
    fun onPageChanged(page: Page) {
        val chapter = page.chapter
        chapter.last_page_read = page.index
        if (chapter.pages!!.last() === page) {
            chapter.read = true
        }
        if (!chapter.isDownloaded && page.status == Page.QUEUE) {
            loader.loadPriorizedPage(page)
        }
    }

    /**
     * Delete selected chapter
     *
     * @param chapter chapter that is selected
     * @param manga manga that belongs to chapter
     */
    fun deleteChapter(chapter: ReaderChapter, manga: Manga) {
        chapter.isDownloaded = false
        chapter.pages?.forEach { it.status == Page.QUEUE }
        downloadManager.deleteChapter(source, manga, chapter)
    }

    /**
     * Returns the chapter to be marked as last read in sync services or 0 if no update required.
     */
    fun getTrackChapterToUpdate(): Int {
        val trackList = trackList
        if (chapter.pages == null || trackList == null || trackList.isEmpty())
            return 0

        val prevChapter = prevChapter

        // Get the last chapter read from the reader.
        val lastChapterRead = if (chapter.read)
            Math.floor(chapter.chapter_number.toDouble()).toInt()
        else if (prevChapter != null && prevChapter.read)
            Math.floor(prevChapter.chapter_number.toDouble()).toInt()
        else
            0

        trackList.forEach { sync ->
            if (lastChapterRead > sync.last_chapter_read) {
                sync.last_chapter_read = lastChapterRead
                sync.update = true
            }
        }

        return if (trackList.any { it.update }) lastChapterRead else 0
    }

    /**
     * Starts the service that updates the last chapter read in sync services
     */
    fun updateTrackLastChapterRead() {
        trackList?.forEach { sync ->
            val service = trackManager.getService(sync.sync_id)
            if (service != null && service.isLogged && sync.update) {
                TrackUpdateService.start(context, sync)
            }
        }
    }

    /**
     * Loads the next chapter.
     *
     * @return true if the next chapter is being loaded, false if there is no next chapter.
     */
    fun loadNextChapter(): Boolean {
        nextChapter?.let {
            onChapterLeft()
            loadChapter(it, 0)
            return true
        }
        return false
    }

    /**
     * Loads the next chapter.
     *
     * @return true if the previous chapter is being loaded, false if there is no previous chapter.
     */
    fun loadPreviousChapter(): Boolean {
        prevChapter?.let {
            onChapterLeft()
            loadChapter(it, if (it.read) -1 else 0)
            return true
        }
        return false
    }

    /**
     * Returns true if there's a next chapter.
     */
    fun hasNextChapter(): Boolean {
        return nextChapter != null
    }

    /**
     * Returns true if there's a previous chapter.
     */
    fun hasPreviousChapter(): Boolean {
        return prevChapter != null
    }

    /**
     * Updates the viewer for this manga.
     *
     * @param viewer the id of the viewer to set.
     */
    fun updateMangaViewer(viewer: Int) {
        manga.viewer = viewer
        db.insertManga(manga).executeAsBlocking()
    }

    /**
     * Update cover with page file.
     */
    internal fun setImageAsCover(page: Page) {
        try {
            val thumbUrl = manga.thumbnail_url ?: throw Exception("Image url not found")
            if (manga.favorite) {
                val input = context.contentResolver.openInputStream(page.uri)
                coverCache.copyToCache(thumbUrl, input)
                context.toast(R.string.cover_updated)
            } else {
                context.toast(R.string.notification_first_add_to_library)
            }
        } catch (error: Exception) {
            context.toast(R.string.notification_cover_update_failed)
            Timber.e(error)
        }
    }

    /**
     * Save page to local storage.
     */
    internal fun savePage(page: Page) {
        if (page.status != Page.READY)
            return

        // Used to show image notification.
        val imageNotifier = ImageNotifier(context)

        // Remove the notification if it already exists (user feedback).
        imageNotifier.onClear()

        // Pictures directory.
        val pictureDirectory = Environment.getExternalStorageDirectory().absolutePath +
                File.separator + Environment.DIRECTORY_PICTURES +
                File.separator + context.getString(R.string.app_name)

        // Copy file in background.
        Observable
                .fromCallable {
                    // Folder where the image will be saved.
                    val destDir = File(pictureDirectory)
                    destDir.mkdirs()

                    // Find out file mime type.
                    val mime = context.contentResolver.getType(page.uri)
                    ?: context.contentResolver.openInputStream(page.uri).buffered().use {
                        URLConnection.guessContentTypeFromStream(it)
                    }

                    // Build destination file.
                    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
                    val filename = DiskUtil.buildValidFilename(
                            "${manga.title} - ${chapter.name}") + " - ${page.number}.$ext"
                    val destFile = File(destDir, filename)

                    context.contentResolver.openInputStream(page.uri).use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    imageNotifier.onComplete(destFile)
                }
                .subscribeOn(Schedulers.io())
                .subscribe({},
                        { error ->
                            Timber.e(error)
                            imageNotifier.onError(error.message)
                        })
    }
}
