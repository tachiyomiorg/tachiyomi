package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.os.Bundle
import android.os.Environment
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.util.DiskUtil
import eu.kanade.tachiyomi.util.ImageUtil
import rx.Completable
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

class ReaderPresenter(
        private val db: DatabaseHelper = Injekt.get(),
        private val sourceManager: SourceManager = Injekt.get(),
        private val downloadManager: DownloadManager = Injekt.get(),
        private val coverCache: CoverCache = Injekt.get(),
        val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<ReaderActivity>() {

    var manga: Manga? = null
        private set

    private var chapterId = -1L

    private var loader: ChapterLoader? = null

    private var activeChapterSubscription: Subscription? = null

    private val viewerChaptersRelay = BehaviorRelay.create<ViewerChapters>()

    private val isLoadingAdjacentChapterRelay = BehaviorRelay.create<Boolean>()

    /**
     * Chapter list for the active manga. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val chapterList by lazy {
        val manga = manga!!
        val dbChapters = db.getChapters(manga).executeAsBlocking()
        val selectedChapter = dbChapters.find { it.id == chapterId }
            ?: error("Requested chapter of id $chapterId not found in chapter list")

        when (manga.sorting) {
            Manga.SORTING_SOURCE -> ChapterLoadBySource().get(dbChapters)
            Manga.SORTING_NUMBER -> ChapterLoadByNumber().get(dbChapters, selectedChapter)
            else -> error("Unknown sorting method")
        }.map(::ReaderChapter)
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        if (savedState != null) {
            chapterId = savedState.getLong(::chapterId.name, -1)
        }
    }

    override fun onSave(state: Bundle) {
        super.onSave(state)
        val currentChapter = viewerChaptersRelay.value?.currChapter
        if (currentChapter != null) {
            currentChapter.requestedPage = currentChapter.chapter.last_page_read
            state.putLong(::chapterId.name, currentChapter.chapter.id!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val currentChapters = viewerChaptersRelay.value
        if (currentChapters != null) {
            currentChapters.unref()
            saveChapterProgress(currentChapters.currChapter)
        }
    }

    fun needsInit(): Boolean {
        return manga == null
    }

    fun init(manga: Manga, initialChapterId: Long) {
        if (!needsInit()) return

        this.manga = manga
        if (chapterId == -1L) chapterId = initialChapterId

        val source = sourceManager.getOrStub(manga.source)
        loader = ChapterLoader(downloadManager, manga, source)

        Observable.just(manga).subscribeLatestCache(ReaderActivity::setManga)
        viewerChaptersRelay.subscribeLatestCache(ReaderActivity::setChapters)
        isLoadingAdjacentChapterRelay.subscribeLatestCache(ReaderActivity::setProgressBar)

        // Read chapterList from an io thread because it's retrieved lazily and would block main.
        activeChapterSubscription?.unsubscribe()
        activeChapterSubscription = Observable
            .fromCallable { chapterList.first { chapterId == it.chapter.id } }
            .flatMap { getLoadObservable(loader!!, it) }
            .subscribeOn(Schedulers.io())
            .subscribeFirst({ _, _ ->
                // Ignore onNext event
            }, ReaderActivity::setInitialChapterError)
    }

    private fun getLoadObservable(
            loader: ChapterLoader,
            chapter: ReaderChapter
    ): Observable<ViewerChapters> {
        return loader.loadChapter(chapter)
            .andThen(Observable.fromCallable {
                val chapterPos = chapterList.indexOf(chapter)

                ViewerChapters(chapter,
                        chapterList.getOrNull(chapterPos - 1),
                        chapterList.getOrNull(chapterPos + 1))
            })
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { newChapters ->
                val oldChapters = viewerChaptersRelay.value

                // Add new references first to avoid unnecessary recycling
                newChapters.ref()
                oldChapters?.unref()

                viewerChaptersRelay.call(newChapters)
            }
    }

    private fun loadNewChapter(chapter: ReaderChapter) {
        val loader = loader ?: return

        Timber.w("Loading ${chapter.chapter.url}")

        activeChapterSubscription?.unsubscribe()
        activeChapterSubscription = getLoadObservable(loader, chapter)
            .toCompletable()
            .onErrorComplete()
            .subscribe()
            .also(::add)
    }

    private fun loadAdjacent(chapter: ReaderChapter) {
        val loader = loader ?: return

        Timber.w("Loading adjacent ${chapter.chapter.url}")

        activeChapterSubscription?.unsubscribe()
        activeChapterSubscription = getLoadObservable(loader, chapter)
            .doOnSubscribe { isLoadingAdjacentChapterRelay.call(true) }
            .doOnUnsubscribe { isLoadingAdjacentChapterRelay.call(false) }
            .subscribeFirst({ view, _ ->
                view.moveToPageIndex(0)
            }, { _, _ ->
                // Ignore onError event, viewers handle that state
            })
    }

    private fun preload(chapter: ReaderChapter) {
        if (chapter.state != ReaderChapter.State.Wait && chapter.state !is ReaderChapter.State.Error) {
            return
        }

        Timber.w("Preloading ${chapter.chapter.url}")

        val loader = loader ?: return

        loader.loadChapter(chapter)
            .observeOn(AndroidSchedulers.mainThread())
            // Update current chapters whenever a chapter is preloaded
            .doOnCompleted { viewerChaptersRelay.value?.let(viewerChaptersRelay::call) }
            .onErrorComplete()
            .subscribe()
            .also(::add)
    }

    fun onPageSelected(page: ReaderPage) {
        val currentChapters = viewerChaptersRelay.value ?: return

        val selectedChapter = page.chapter

        // Save last page read and mark as read if needed
        selectedChapter.chapter.last_page_read = page.index
        if (selectedChapter.pages?.lastIndex == page.index) {
            selectedChapter.chapter.read = true
        }

        if (selectedChapter != currentChapters.currChapter) {
            Timber.w("Setting ${selectedChapter.chapter.url} as active")
            onChapterChanged(currentChapters.currChapter, selectedChapter)
            loadNewChapter(selectedChapter)
        }
    }

    private fun onChapterChanged(fromChapter: ReaderChapter, toChapter: ReaderChapter) {
        saveChapterProgress(fromChapter)
        saveChapterHistory(fromChapter)
    }

    private fun saveChapterProgress(chapter: ReaderChapter) {
        db.updateChapterProgress(chapter.chapter).asRxCompletable()
            .onErrorComplete()
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    private fun saveChapterHistory(chapter: ReaderChapter) {
        val history = History.create(chapter.chapter).apply { last_read = Date().time }
        db.updateHistoryLastRead(history).asRxCompletable()
            .onErrorComplete()
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Called when the application is going to background
     */
    fun saveCurrentProgress() {
        val currentChapters = viewerChaptersRelay.value ?: return
        saveChapterProgress(currentChapters.currChapter)
    }

    fun preloadNextChapter() {
        val nextChapter = viewerChaptersRelay.value?.nextChapter ?: return
        preload(nextChapter)
    }

    fun preloadPreviousChapter() {
        val prevChapter = viewerChaptersRelay.value?.prevChapter ?: return
        preload(prevChapter)
    }

    fun loadNextChapter(): Boolean {
        val nextChapter = viewerChaptersRelay.value?.nextChapter ?: return false
        loadAdjacent(nextChapter)
        return true
    }

    fun loadPreviousChapter(): Boolean {
        val prevChapter = viewerChaptersRelay.value?.prevChapter ?: return false
        loadAdjacent(prevChapter)
        return true
    }

    fun getCurrentChapter(): ReaderChapter? {
        return viewerChaptersRelay.value?.currChapter
    }

    fun getMangaViewer(): Int {
        val manga = manga ?: return preferences.defaultViewer()
        return if (manga.viewer == 0) preferences.defaultViewer() else manga.viewer
    }

    fun setMangaViewer(viewer: Int) {
        val manga = manga ?: return
        manga.viewer = viewer
        // TODO custom put operation
        db.insertManga(manga).executeAsBlocking()

        Observable.timer(250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .subscribeFirst({ view, _ ->
                val currChapters = viewerChaptersRelay.value
                if (currChapters != null) {
                    // Save current page
                    val currChapter = currChapters.currChapter
                    currChapter.requestedPage = currChapter.chapter.last_page_read

                    // Emit manga and chapters to the new viewer
                    view.setManga(manga)
                    view.setChapters(currChapters)
                }
            })
    }

    private fun saveImage(page: ReaderPage, directory: File, manga: Manga): File {
        val stream = page.stream!!
        val type = ImageUtil.findImageType(stream) ?: throw Exception("Not an image")

        directory.mkdirs()

        val chapter = page.chapter.chapter

        // Build destination file.
        val filename = DiskUtil.buildValidFilename(
                "${manga.title} - ${chapter.name}") + " - ${page.number}.${type.extension}"

        val destFile = File(directory, filename)
        stream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    fun saveImage(page: ReaderPage) {
        if (page.status != Page.READY) return
        val manga = manga ?: return
        val context = Injekt.get<Application>()

        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        // Pictures directory.
        val destDir = File(Environment.getExternalStorageDirectory().absolutePath +
                           File.separator + Environment.DIRECTORY_PICTURES +
                           File.separator + "Tachiyomi")

        // Copy file in background.
        Observable.fromCallable { saveImage(page, destDir, manga) }
            .doOnNext { file ->
                DiskUtil.scanMedia(context, file)
                notifier.onComplete(file)
            }
            .doOnError { notifier.onError(it.message) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                    { view, file -> view.onSaveImageResult(SaveImageResult.Success(file)) },
                    { view, error -> view.onSaveImageResult(SaveImageResult.Error(error)) }
            )
    }

    fun shareImage(page: ReaderPage) {
        if (page.status != Page.READY) return
        val manga = manga ?: return
        val context = Injekt.get<Application>()

        val destDir = File(context.cacheDir, "shared_image")

        Observable.fromCallable { destDir.delete() } // Keep only the last shared file
            .map { saveImage(page, destDir, manga) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                    { view, file -> view.onShareImageResult(file) },
                    { view, error -> /* Empty */ }
            )
    }

    fun setAsCover(page: ReaderPage) {
        if (page.status != Page.READY) return
        val manga = manga ?: return
        val stream = page.stream ?: return

        Observable
            .fromCallable {
                if (manga.source == LocalSource.ID) {
                    val context = Injekt.get<Application>()
                    LocalSource.updateCover(context, manga, stream())
                    R.string.cover_updated
                    SetAsCoverResult.Success
                } else {
                    val thumbUrl = manga.thumbnail_url ?: throw Exception("Image url not found")
                    if (manga.favorite) {
                        coverCache.copyToCache(thumbUrl, stream())
                        SetAsCoverResult.Success
                    } else {
                        SetAsCoverResult.AddToLibraryFirst
                    }
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                    { view, result -> view.onSetAsCoverResult(result) },
                    { view, _ -> view.onSetAsCoverResult(SetAsCoverResult.Error) }
            )
    }

    enum class SetAsCoverResult {
        Success, AddToLibraryFirst, Error
    }

    sealed class SaveImageResult {
        class Success(val file: File) : SaveImageResult()
        class Error(val error: Throwable) : SaveImageResult()
    }

    /**
     * Starts the service that updates the last chapter read in sync services
     */
    fun updateTrackLastChapterRead() {
        if (!preferences.autoUpdateTrack()) return
        val viewerChapters = viewerChaptersRelay.value ?: return
        val manga = manga ?: return

        val currChapter = viewerChapters.currChapter.chapter
        val prevChapter = viewerChapters.prevChapter?.chapter

        // Get the last chapter read from the reader.
        val lastChapterRead = if (currChapter.read)
            currChapter.chapter_number.toInt()
        else if (prevChapter != null && prevChapter.read)
            prevChapter.chapter_number.toInt()
        else
            return

        val trackManager = Injekt.get<TrackManager>()

        db.getTracks(manga).asRxSingle()
            .flatMapCompletable { trackList ->
                Completable.concat(trackList.map { track ->
                    val service = trackManager.getService(track.sync_id)
                    if (service != null && service.isLogged && lastChapterRead > track.last_chapter_read) {
                        track.last_chapter_read = lastChapterRead

                        // We wan't these to execute even if the presenter is destroyed and leaks
                        // for a while. The view can still be garbage collected.
                        Observable.defer { service.update(track) }
                            .map { db.insertTrack(track).executeAsBlocking() }
                            .toCompletable()
                            .onErrorComplete()
                    } else {
                        Completable.complete()
                    }
                })
            }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

}
