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
import eu.kanade.tachiyomi.ui.reader.loader.DownloadPageLoader
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
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Presenter used by the activity to perform background operations.
 */
class ReaderPresenter(
        private val db: DatabaseHelper = Injekt.get(),
        private val sourceManager: SourceManager = Injekt.get(),
        private val downloadManager: DownloadManager = Injekt.get(),
        private val coverCache: CoverCache = Injekt.get(),
        private val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<ReaderActivity>() {

    /**
     * The manga loaded in the reader. It can be null when instantiated for a short time.
     */
    var manga: Manga? = null
        private set

    /**
     * The chapter id of the currently loaded chapter. Used to restore from process kill.
     */
    private var chapterId = -1L

    /**
     * The chapter loader for the loaded manga. It'll be null until [manga] is set.
     */
    private var loader: ChapterLoader? = null

    /**
     * Subscription to prevent setting chapters as active from multiple threads.
     */
    private var activeChapterSubscription: Subscription? = null

    /**
     * Relay for currently active viewer chapters.
     */
    private val viewerChaptersRelay = BehaviorRelay.create<ViewerChapters>()

    /**
     * Relay used when loading prev/next chapter needed to lock the UI (with a dialog).
     */
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

        val chaptersForReader =
                if (preferences.skipRead()) {
                    var list = dbChapters.filter { it -> !it.read }.toMutableList()
                    val find = list.find { it.id == chapterId }
                    if (find == null) {
                        list.add(selectedChapter)
                    }
                    list
                } else {
                    dbChapters
                }

        when (manga.sorting) {
            Manga.SORTING_SOURCE -> ChapterLoadBySource().get(chaptersForReader)
            Manga.SORTING_NUMBER -> ChapterLoadByNumber().get(chaptersForReader, selectedChapter)
            else -> error("Unknown sorting method")
        }.map(::ReaderChapter)
    }

    /**
     * Called when the presenter is created. It retrieves the saved active chapter if the process
     * was restored.
     */
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        if (savedState != null) {
            chapterId = savedState.getLong(::chapterId.name, -1)
        }
    }

    /**
     * Called when the presenter is destroyed. It saves the current progress and cleans up
     * references on the currently active chapters.
     */
    override fun onDestroy() {
        super.onDestroy()
        val currentChapters = viewerChaptersRelay.value
        if (currentChapters != null) {
            currentChapters.unref()
            saveChapterProgress(currentChapters.currChapter)
            saveChapterHistory(currentChapters.currChapter)
        }
    }

    /**
     * Called when the presenter instance is being saved. It saves the currently active chapter
     * id and the last page read.
     */
    override fun onSave(state: Bundle) {
        super.onSave(state)
        val currentChapter = getCurrentChapter()
        if (currentChapter != null) {
            currentChapter.requestedPage = currentChapter.chapter.last_page_read
            state.putLong(::chapterId.name, currentChapter.chapter.id!!)
        }
    }

    /**
     * Called when the user pressed the back button and is going to leave the reader. Used to
     * trigger deletion of the downloaded chapters.
     */
    fun onBackPressed() {
        deletePendingChapters()
    }

    /**
     * Called when the activity is saved and not changing configurations. It updates the database
     * to persist the current progress of the active chapter.
     */
    fun onSaveInstanceStateNonConfigurationChange() {
        val currentChapter = getCurrentChapter() ?: return
        saveChapterProgress(currentChapter)
    }

    /**
     * Whether this presenter is initialized yet.
     */
    fun needsInit(): Boolean {
        return manga == null
    }

    /**
     * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
     * fetch the manga from the database and initialize the initial chapter.
     */
    fun init(mangaId: Long, initialChapterId: Long) {
        if (!needsInit()) return

        db.getManga(mangaId).asRxObservable()
                .first()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { init(it, initialChapterId) }
                .subscribeFirst({ _, _ ->
                    // Ignore onNext event
                }, ReaderActivity::setInitialChapterError)
    }

    /**
     * Initializes this presenter with the given [manga] and [initialChapterId]. This method will
     * set the chapter loader, view subscriptions and trigger an initial load.
     */
    private fun init(manga: Manga, initialChapterId: Long) {
        if (!needsInit()) return

        this.manga = manga
        if (chapterId == -1L) chapterId = initialChapterId

        val source = sourceManager.getOrStub(manga.source)
        loader = ChapterLoader(downloadManager, manga, source)

        Observable.just(manga).subscribeLatestCache(ReaderActivity::setManga)
        viewerChaptersRelay.subscribeLatestCache(ReaderActivity::setChapters)
        isLoadingAdjacentChapterRelay.subscribeLatestCache(ReaderActivity::setProgressDialog)

        // Read chapterList from an io thread because it's retrieved lazily and would block main.
        activeChapterSubscription?.unsubscribe()
        activeChapterSubscription = Observable
                .fromCallable { chapterList.first { chapterId == it.chapter.id } }
                .flatMap { getLoadObservable(loader!!, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst({ _, _ ->
                    // Ignore onNext event
                }, ReaderActivity::setInitialChapterError)
    }

    /**
     * Returns an observable that loads the given [chapter] with this [loader]. This observable
     * handles main thread synchronization and updating the currently active chapters on
     * [viewerChaptersRelay], however callers must ensure there won't be more than one
     * subscription active by unsubscribing any existing [activeChapterSubscription] before.
     * Callers must also handle the onError event.
     */
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

    /**
     * Called when the user changed to the given [chapter] when changing pages from the viewer.
     * It's used only to set this chapter as active.
     */
    private fun loadNewChapter(chapter: ReaderChapter) {
        val loader = loader ?: return

        Timber.d("Loading ${chapter.chapter.url}")

        activeChapterSubscription?.unsubscribe()
        activeChapterSubscription = getLoadObservable(loader, chapter)
                .toCompletable()
                .onErrorComplete()
                .subscribe()
                .also(::add)
    }

    /**
     * Called when the user is going to load the prev/next chapter through the menu button. It
     * sets the [isLoadingAdjacentChapterRelay] that the view uses to prevent any further
     * interaction until the chapter is loaded.
     */
    private fun loadAdjacent(chapter: ReaderChapter) {
        val loader = loader ?: return

        Timber.d("Loading adjacent ${chapter.chapter.url}")

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

    /**
     * Called when the viewers decide it's a good time to preload a [chapter] and improve the UX so
     * that the user doesn't have to wait too long to continue reading.
     */
    private fun preload(chapter: ReaderChapter) {
        if (chapter.state != ReaderChapter.State.Wait && chapter.state !is ReaderChapter.State.Error) {
            return
        }

        Timber.d("Preloading ${chapter.chapter.url}")

        val loader = loader ?: return

        loader.loadChapter(chapter)
                .observeOn(AndroidSchedulers.mainThread())
                // Update current chapters whenever a chapter is preloaded
                .doOnCompleted { viewerChaptersRelay.value?.let(viewerChaptersRelay::call) }
                .onErrorComplete()
                .subscribe()
                .also(::add)
    }

    /**
     * Called every time a page changes on the reader. Used to mark the flag of chapters being
     * read, update tracking services, enqueue downloaded chapter deletion, and updating the active chapter if this
     * [page]'s chapter is different from the currently active.
     */
    fun onPageSelected(page: ReaderPage) {
        val currentChapters = viewerChaptersRelay.value ?: return

        val selectedChapter = page.chapter

        // Save last page read and mark as read if needed
        selectedChapter.chapter.last_page_read = page.index
        if (selectedChapter.pages?.lastIndex == page.index) {
            selectedChapter.chapter.read = true
            updateTrackLastChapterRead()
            enqueueDeleteReadChapters(selectedChapter)
        }

        if (selectedChapter != currentChapters.currChapter) {
            Timber.d("Setting ${selectedChapter.chapter.url} as active")
            onChapterChanged(currentChapters.currChapter, selectedChapter)
            loadNewChapter(selectedChapter)
        }
    }

    /**
     * Called when a chapter changed from [fromChapter] to [toChapter]. It updates [fromChapter]
     * on the database.
     */
    private fun onChapterChanged(fromChapter: ReaderChapter, toChapter: ReaderChapter) {
        saveChapterProgress(fromChapter)
        saveChapterHistory(fromChapter)
    }

    /**
     * Saves this [chapter] progress (last read page and whether it's read).
     */
    private fun saveChapterProgress(chapter: ReaderChapter) {
        db.updateChapterProgress(chapter.chapter).asRxCompletable()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    /**
     * Saves this [chapter] last read history.
     */
    private fun saveChapterHistory(chapter: ReaderChapter) {
        val history = History.create(chapter.chapter).apply { last_read = Date().time }
        db.updateHistoryLastRead(history).asRxCompletable()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    /**
     * Called from the activity to preload the given [chapter].
     */
    fun preloadChapter(chapter: ReaderChapter) {
        preload(chapter)
    }

    /**
     * Called from the activity to load and set the next chapter as active.
     */
    fun loadNextChapter() {
        val nextChapter = viewerChaptersRelay.value?.nextChapter ?: return
        loadAdjacent(nextChapter)
    }

    /**
     * Called from the activity to load and set the previous chapter as active.
     */
    fun loadPreviousChapter() {
        val prevChapter = viewerChaptersRelay.value?.prevChapter ?: return
        loadAdjacent(prevChapter)
    }

    /**
     * Returns the currently active chapter.
     */
    fun getCurrentChapter(): ReaderChapter? {
        return viewerChaptersRelay.value?.currChapter
    }

    /**
     * Returns the viewer position used by this manga or the default one.
     */
    fun getMangaViewer(): Int {
        val manga = manga ?: return preferences.defaultViewer()
        return if (manga.viewer == 0) preferences.defaultViewer() else manga.viewer
    }

    /**
     * Updates the viewer position for the open manga.
     */
    fun setMangaViewer(viewer: Int) {
        val manga = manga ?: return
        manga.viewer = viewer
        db.updateMangaViewer(manga).executeAsBlocking()

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

    /**
     * Saves the image of this [page] in the given [directory] and returns the file location.
     */
    private fun saveImage(page: ReaderPage, directory: File, manga: Manga): File {
        val stream = page.stream!!
        val type = ImageUtil.findImageType(stream) ?: throw Exception("Not an image")

        directory.mkdirs()

        val chapter = page.chapter.chapter

        // Build destination file.
        val filename = DiskUtil.buildValidFilename(
                "${manga.title} - ${chapter.name}".take(225)
        ) + " - ${page.number}.${type.extension}"

        val destFile = File(directory, filename)
        stream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    /**
     * Saves the image of this [page] on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
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

    /**
     * Shares the image of this [page] and notifies the UI with the path of the file to share.
     * The image must be first copied to the internal partition because there are many possible
     * formats it can come from, like a zipped chapter, in which case it's not possible to directly
     * get a path to the file and it has to be decompresssed somewhere first. Only the last shared
     * image will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(page: ReaderPage) {
        if (page.status != Page.READY) return
        val manga = manga ?: return
        val context = Injekt.get<Application>()

        val destDir = File(context.cacheDir, "shared_image")

        Observable.fromCallable { destDir.deleteRecursively() } // Keep only the last shared file
                .map { saveImage(page, destDir, manga) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst(
                        { view, file -> view.onShareImageResult(file) },
                        { view, error -> /* Empty */ }
                )
    }

    /**
     * Sets the image of this [page] as cover and notifies the UI of the result.
     */
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

    /**
     * Results of the set as cover feature.
     */
    enum class SetAsCoverResult {
        Success, AddToLibraryFirst, Error
    }

    /**
     * Results of the save image feature.
     */
    sealed class SaveImageResult {
        class Success(val file: File) : SaveImageResult()
        class Error(val error: Throwable) : SaveImageResult()
    }

    /**
     * Starts the service that updates the last chapter read in sync services. This operation
     * will run in a background thread and errors are ignored.
     */
    private fun updateTrackLastChapterRead() {
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
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    /**
     * Enqueues this [chapter] to be deleted when [deletePendingChapters] is called. The download
     * manager handles persisting it across process deaths.
     */
    private fun enqueueDeleteReadChapters(chapter: ReaderChapter) {
        if (!chapter.chapter.read || chapter.pageLoader !is DownloadPageLoader) return
        val manga = manga ?: return

        // Return if the setting is disabled
        val removeAfterReadSlots = preferences.removeAfterReadSlots()
        if (removeAfterReadSlots == -1) return

        Completable
                .fromCallable {
                    // Position of the read chapter
                    val position = chapterList.indexOf(chapter)

                    // Retrieve chapter to delete according to preference
                    val chapterToDelete = chapterList.getOrNull(position - removeAfterReadSlots)
                    if (chapterToDelete != null) {
                        downloadManager.enqueueDeleteChapters(listOf(chapterToDelete.chapter), manga)
                    }
                }
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    /**
     * Deletes all the pending chapters. This operation will run in a background thread and errors
     * are ignored.
     */
    private fun deletePendingChapters() {
        Completable.fromCallable { downloadManager.deletePendingChapters() }
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

}
