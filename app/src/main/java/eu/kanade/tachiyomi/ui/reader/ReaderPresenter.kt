package eu.kanade.tachiyomi.ui.reader

import android.os.Bundle
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.data.mangasync.UpdateMangaSyncService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.base.OnlineSource
import eu.kanade.tachiyomi.data.source.base.Source
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.SharedData
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class ReaderPresenter : BasePresenter<ReaderActivity>() {

    @Inject lateinit var prefs: PreferencesHelper
    @Inject lateinit var db: DatabaseHelper
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var syncManager: MangaSyncManager
    @Inject lateinit var sourceManager: SourceManager
    @Inject lateinit var chapterCache: ChapterCache

    lateinit var manga: Manga
        private set

    lateinit var chapter: Chapter
        private set

    lateinit var source: Source
        private set

    var requestedPage: Int = 0
    var currentPage: Page? = null
    private var nextChapter: Chapter? = null
    private var previousChapter: Chapter? = null
    private var mangaSyncList: List<MangaSync>? = null

    private val retryPageSubject by lazy { PublishSubject.create<Page>() }
    private val pageInitializerSubject by lazy { PublishSubject.create<Chapter>() }

    val isSeamlessMode by lazy { prefs.seamlessMode() }

    private var appenderSubscription: Subscription? = null

    private val GET_PAGE_LIST = 1
    private val GET_ADJACENT_CHAPTERS = 2
    private val GET_MANGA_SYNC = 3
    private val PRELOAD_NEXT_CHAPTER = 4

    private val MANGA_KEY = "manga_key"
    private val CHAPTER_KEY = "chapter_key"
    private val PAGE_KEY = "page_key"

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (savedState == null) {
            val event = SharedData.get(ReaderEvent::class.java) ?: return
            manga = event.manga
            chapter = event.chapter
        } else {
            manga = savedState.getSerializable(MANGA_KEY) as Manga
            chapter = savedState.getSerializable(CHAPTER_KEY) as Chapter
            requestedPage = savedState.getInt(PAGE_KEY)
        }

        source = sourceManager.get(manga.source)!!

        initializeSubjects()

        startableLatestCache(GET_ADJACENT_CHAPTERS,
                { getAdjacentChaptersObservable() },
                { view, pair -> view.onAdjacentChapters(pair.first, pair.second) })

        startable(PRELOAD_NEXT_CHAPTER,
                { getPreloadNextChapterObservable() },
                { },
                { error -> Timber.e("Error preloading chapter") })


        restartable(GET_MANGA_SYNC,
                { getMangaSyncObservable().subscribe() })

        restartableLatestCache(GET_PAGE_LIST,
                { getPageListObservable(chapter) },
                { view, chapter -> view.onChapterReady(manga, this.chapter, currentPage) },
                { view, error -> view.onChapterError(error) })

        if (savedState == null) {
            loadChapter(chapter)
            if (prefs.autoUpdateMangaSync()) {
                start(GET_MANGA_SYNC)
            }
        }
    }

    override fun onSave(state: Bundle) {
        onChapterLeft()
        state.putSerializable(MANGA_KEY, manga)
        state.putSerializable(CHAPTER_KEY, chapter)
        state.putSerializable(PAGE_KEY, currentPage?.pageNumber ?: 0)
        super.onSave(state)
    }

    private fun initializeSubjects() {
        // Listen for pages initialization events
        add(pageInitializerSubject.observeOn(Schedulers.io())
                .concatMap { ch ->
                    val observable: Observable<Page>
                    if (ch.isDownloaded) {
                        val chapterDir = downloadManager.getAbsoluteChapterDirectory(source, manga, ch)
                        observable = Observable.from(ch.pages)
                                .flatMap { downloadManager.getDownloadedImage(it, chapterDir) }
                    } else {
                        observable = source.let { source ->
                            if (source is OnlineSource) {
                                source.fetchAllImageUrlsFromPageList(ch.pages)
                                        .flatMap({ source.getCachedImage(it) }, 2)
                                        .doOnCompleted { source.savePageList(ch, ch.pages) }
                            } else {
                                Observable.from(ch.pages)
                                        .flatMap { source.fetchImage(it) }
                            }
                        }
                    }
                    observable.doOnCompleted {
                        if (!isSeamlessMode && chapter === ch) {
                            preloadNextChapter()
                        }
                    }
                }.subscribe())

        // Listen por retry events
        add(retryPageSubject.observeOn(Schedulers.io())
                .flatMap { source.fetchImage(it) }
                .subscribe())
    }

    // Returns the page list of a chapter
    private fun getPageListObservable(chapter: Chapter): Observable<Chapter> {
        val observable: Observable<List<Page>> = if (chapter.isDownloaded)
        // Fetch the page list from disk
            Observable.just(downloadManager.getSavedPageList(source, manga, chapter)!!)
        else
        // Fetch the page list from cache or fallback to network
            source.fetchPageList(chapter)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())

        return observable.map { pages ->
            for (page in pages) {
                page.chapter = chapter
            }
            chapter.pages = pages
            if (requestedPage >= -1 || currentPage == null) {
                if (requestedPage == -1) {
                    currentPage = pages[pages.size - 1]
                } else {
                    currentPage = pages[requestedPage]
                }
            }
            requestedPage = -2
            pageInitializerSubject.onNext(chapter)
            chapter
        }
    }

    private fun getAdjacentChaptersObservable(): Observable<Pair<Chapter, Chapter>> {
        val strategy = getAdjacentChaptersStrategy()
        return Observable.zip(strategy.first, strategy.second) { prev, next -> Pair(prev, next) }
                .doOnNext { pair ->
                    previousChapter = pair.first
                    nextChapter = pair.second
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    private fun getAdjacentChaptersStrategy() = when (manga.sorting) {
        Manga.SORTING_NUMBER -> Pair(
                db.getPreviousChapter(chapter).asRxObservable().take(1),
                db.getNextChapter(chapter).asRxObservable().take(1))
        Manga.SORTING_SOURCE -> Pair(
                db.getPreviousChapterBySource(chapter).asRxObservable().take(1),
                db.getNextChapterBySource(chapter).asRxObservable().take(1))
        else -> throw AssertionError("Unknown sorting method")
    }

    // Preload the first pages of the next chapter. Only for non seamless mode
    private fun getPreloadNextChapterObservable(): Observable<Page> {
        val nextChapter = nextChapter ?: return Observable.error(Exception("No next chapter"))
        return source.fetchPageList(nextChapter)
                .flatMap { pages ->
                    nextChapter.pages = pages
                    val pagesToPreload = Math.min(pages.size, 5)
                    Observable.from(pages).take(pagesToPreload)
                }
                // Preload up to 5 images
                .concatMap { source.fetchImage(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted { stopPreloadingNextChapter() }
    }

    private fun getMangaSyncObservable(): Observable<List<MangaSync>> {
        return db.getMangasSync(manga).asRxObservable()
                .take(1)
                .doOnNext { mangaSyncList = it }
    }

    // Loads the given chapter
    private fun loadChapter(chapter: Chapter, requestedPage: Int = 0) {
        if (isSeamlessMode) {
            if (appenderSubscription != null)
                remove(appenderSubscription)
        } else {
            stopPreloadingNextChapter()
        }

        this.chapter = chapter
        chapter.status = if (isChapterDownloaded(chapter)) Download.DOWNLOADED else Download.NOT_DOWNLOADED

        // If the chapter is partially read, set the starting page to the last the user read
        if (!chapter.read && chapter.last_page_read != 0)
            this.requestedPage = chapter.last_page_read
        else
            this.requestedPage = requestedPage

        // Reset next and previous chapter. They have to be fetched again
        nextChapter = null
        previousChapter = null

        start(GET_PAGE_LIST)
        start(GET_ADJACENT_CHAPTERS)
    }

    fun setActiveChapter(chapter: Chapter) {
        onChapterLeft()
        this.chapter = chapter
        nextChapter = null
        previousChapter = null
        start(GET_ADJACENT_CHAPTERS)
    }

    fun appendNextChapter() {
        if (nextChapter == null)
            return

        if (appenderSubscription != null)
            remove(appenderSubscription)

        nextChapter?.let {
            if (appenderSubscription != null)
                remove(appenderSubscription)

            it.status = if (isChapterDownloaded(it)) Download.DOWNLOADED else Download.NOT_DOWNLOADED

            appenderSubscription = getPageListObservable(it).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(deliverLatestCache<Chapter>())
                    .subscribe(split({ view, chapter ->
                        view.onAppendChapter(chapter)
                    }, { view, error ->
                        view.onChapterAppendError()
                    }))

            add(appenderSubscription)

        }
    }

    // Check whether the given chapter is downloaded
    fun isChapterDownloaded(chapter: Chapter): Boolean {
        return downloadManager.isChapterDownloaded(source, manga, chapter)
    }

    fun retryPage(page: Page?) {
        if (page != null) {
            page.status = Page.QUEUE
            if (page.imagePath != null) {
                val file = File(page.imagePath)
                chapterCache.removeFileFromCache(file.name)
            }
            retryPageSubject.onNext(page)
        }
    }

    // Called before loading another chapter or leaving the reader. It allows to do operations
    // over the chapter read like saving progress
    fun onChapterLeft() {
        val pages = chapter.pages ?: return

        // Get the last page read
        var activePageNumber = chapter.last_page_read

        // Just in case, avoid out of index exceptions
        if (activePageNumber >= pages.size) {
            activePageNumber = pages.size - 1
        }
        val activePage = pages[activePageNumber]

        // Cache current page list progress for online chapters to allow a faster reopen
        if (!chapter.isDownloaded) {
            source.let { if (it is OnlineSource) it.savePageList(chapter, pages) }
        }

        // Save current progress of the chapter. Mark as read if the chapter is finished
        if (activePage.isLastPage) {
            chapter.read = true

            // Check if remove after read is selected by user
            if (prefs.removeAfterRead()) {
                if (prefs.removeAfterReadPrevious() ) {
                    if (previousChapter != null) {
                        deleteChapter(previousChapter!!, manga)
                    }
                } else {
                    deleteChapter(chapter, manga)
                }
            }
        }
        db.updateChapterProgress(chapter).asRxObservable().subscribe()
    }

    /**
     * Delete selected chapter
     * @param chapter chapter that is selected
     * *
     * @param manga manga that belongs to chapter
     */
    fun deleteChapter(chapter: Chapter, manga: Manga) {
        val source = sourceManager.get(manga.source)!!
        downloadManager.deleteChapter(source, manga, chapter)
    }

    // If the current chapter has been read, we check with this one
    // If not, we check if the previous chapter has been read
    // We know the chapter we have to check, but we don't know yet if an update is required.
    // This boolean is used to return 0 if no update is required
    fun getMangaSyncChapterToUpdate(): Int {
        if (chapter.pages == null || mangaSyncList == null || mangaSyncList!!.isEmpty())
            return 0

        var lastChapterReadLocal = 0
        if (chapter.read)
            lastChapterReadLocal = Math.floor(chapter.chapter_number.toDouble()).toInt()
        else if (previousChapter != null && previousChapter!!.read)
            lastChapterReadLocal = Math.floor(previousChapter!!.chapter_number.toDouble()).toInt()
        var hasToUpdate = false

        for (mangaSync in mangaSyncList!!) {
            if (lastChapterReadLocal > mangaSync.last_chapter_read) {
                mangaSync.last_chapter_read = lastChapterReadLocal
                mangaSync.update = true
                hasToUpdate = true
            }
        }
        return if (hasToUpdate) lastChapterReadLocal else 0
    }

    fun updateMangaSyncLastChapterRead() {
        for (mangaSync in mangaSyncList ?: emptyList()) {
            val service = syncManager.getService(mangaSync.sync_id)
            if (service.isLogged && mangaSync.update) {
                UpdateMangaSyncService.start(context, mangaSync)
            }
        }
    }

    fun loadNextChapter(): Boolean {
        nextChapter?.let {
            onChapterLeft()
            loadChapter(it, 0)
            return true
        }
        return false
    }

    fun loadPreviousChapter(): Boolean {
        previousChapter?.let {
            onChapterLeft()
            loadChapter(it, 0)
            return true
        }
        return false
    }

    fun hasNextChapter(): Boolean {
        return nextChapter != null
    }

    fun hasPreviousChapter(): Boolean {
        return previousChapter != null
    }

    private fun preloadNextChapter() {
        nextChapter?.let {
            if (!isChapterDownloaded(it)) {
                start(PRELOAD_NEXT_CHAPTER)
            }
        }
    }

    private fun stopPreloadingNextChapter() {
        if (!isUnsubscribed(PRELOAD_NEXT_CHAPTER)) {
            stop(PRELOAD_NEXT_CHAPTER)
            nextChapter?.let { chapter ->
                if (chapter.pages != null) {
                    source.let { if (it is OnlineSource) it.savePageList(chapter, chapter.pages) }
                }
            }
        }
    }

    fun updateMangaViewer(viewer: Int) {
        manga.viewer = viewer
        db.insertManga(manga).executeAsBlocking()
    }
}
