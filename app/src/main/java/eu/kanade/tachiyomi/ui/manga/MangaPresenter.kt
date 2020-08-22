package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.isLocal
import eu.kanade.tachiyomi.util.lang.isNullOrUnsubscribed
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.prepUpdateCover
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.shouldDownloadNewChapters
import eu.kanade.tachiyomi.util.updateCoverLastModified
import java.util.Date
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaPresenter(
    val manga: Manga,
    val source: Source,
    val preferences: PreferencesHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get()
) : BasePresenter<MangaController>() {

    /**
     * Subscription to update the manga from the source.
     */
    private var fetchMangaSubscription: Subscription? = null

    /**
     * List of chapters of the manga. It's always unfiltered and unsorted.
     */
    var chapters: List<ChapterItem> = emptyList()
        private set

    /**
     * Subject of list of chapters to allow updating the view without going to DB.
     */
    private val chaptersRelay: PublishRelay<List<ChapterItem>> by lazy {
        PublishRelay.create<List<ChapterItem>>()
    }

    /**
     * Whether the chapter list has been requested to the source.
     */
    var hasRequested = false
        private set

    /**
     * Subscription to retrieve the new list of chapters from the source.
     */
    private var fetchChaptersSubscription: Subscription? = null

    /**
     * Subscription to observe download status changes.
     */
    private var observeDownloadsSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Manga info - start

        getMangaObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache({ view, manga -> view.onNextMangaInfo(manga, source) })

        getTrackingObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(MangaController::onTrackingCount) { _, error -> Timber.e(error) }

        // Prepare the relay.
        chaptersRelay.flatMap { applyChapterFilters(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(MangaController::onNextChapters) { _, error -> Timber.e(error) }

        // Manga info - end

        // Chapters list - start

        // Add the subscription that retrieves the chapters from the database, keeps subscribed to
        // changes, and sends the list of chapters to the relay.
        add(
            db.getChapters(manga).asRxObservable()
                .map { chapters ->
                    // Convert every chapter to a model.
                    chapters.map { it.toModel() }
                }
                .doOnNext { chapters ->
                    // Find downloaded chapters
                    setDownloadedChapters(chapters)

                    // Store the last emission
                    this.chapters = chapters

                    // Listen for download status changes
                    observeDownloads()
                }
                .subscribe { chaptersRelay.call(it) }
        )

        // Chapters list - end
    }

    // Manga info - start

    private fun getMangaObservable(): Observable<Manga> {
        return db.getManga(manga.url, manga.source).asRxObservable()
    }

    private fun getTrackingObservable(): Observable<Int> {
        if (!trackManager.hasLoggedServices()) {
            return Observable.just(0)
        }

        return db.getTracks(manga).asRxObservable()
            .map { tracks ->
                val loggedServices = trackManager.services.filter { it.isLogged }.map { it.id }
                tracks.filter { it.sync_id in loggedServices }
            }
            .map { it.size }
    }

    /**
     * Fetch manga information from source.
     */
    fun fetchMangaFromSource(manualFetch: Boolean = false) {
        if (!fetchMangaSubscription.isNullOrUnsubscribed()) return
        fetchMangaSubscription = Observable.defer { source.fetchMangaDetails(manga) }
            .map { networkManga ->
                manga.prepUpdateCover(coverCache, networkManga, manualFetch)
                manga.copyFrom(networkManga)
                manga.initialized = true
                db.insertManga(manga).executeAsBlocking()
                manga
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ ->
                    view.onFetchMangaInfoDone()
                },
                MangaController::onFetchMangaInfoError
            )
    }

    /**
     * Update favorite status of manga, (removes / adds) manga (to / from) library.
     *
     * @return the new status of the manga.
     */
    fun toggleFavorite(): Boolean {
        manga.favorite = !manga.favorite
        manga.date_added = when (manga.favorite) {
            true -> Date().time
            false -> 0
        }
        if (!manga.favorite) {
            manga.removeCovers(coverCache)
        }
        db.insertManga(manga).executeAsBlocking()
        return manga.favorite
    }

    /**
     * Returns true if the manga has any downloads.
     */
    fun hasDownloads(): Boolean {
        return downloadManager.getDownloadCount(manga) > 0
    }

    /**
     * Deletes all the downloads for the manga.
     */
    fun deleteDownloads() {
        downloadManager.deleteManga(manga, source)
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    fun getCategories(): List<Category> {
        return db.getCategories().executeAsBlocking()
    }

    /**
     * Gets the category id's the manga is in, if the manga is not in a category, returns the default id.
     *
     * @param manga the manga to get categories from.
     * @return Array of category ids the manga is in, if none returns default id
     */
    fun getMangaCategoryIds(manga: Manga): Array<Int> {
        val categories = db.getCategoriesForManga(manga).executeAsBlocking()
        return categories.mapNotNull { it.id }.toTypedArray()
    }

    /**
     * Move the given manga to categories.
     *
     * @param manga the manga to move.
     * @param categories the selected categories.
     */
    fun moveMangaToCategories(manga: Manga, categories: List<Category>) {
        val mc = categories.filter { it.id != 0 }.map { MangaCategory.create(manga, it) }
        db.setMangaCategories(mc, listOf(manga))
    }

    /**
     * Move the given manga to the category.
     *
     * @param manga the manga to move.
     * @param category the selected category, or null for default category.
     */
    fun moveMangaToCategory(manga: Manga, category: Category?) {
        moveMangaToCategories(manga, listOfNotNull(category))
    }

    /**
     * Update cover with local file.
     *
     * @param manga the manga edited.
     * @param context Context.
     * @param data uri of the cover resource.
     */
    fun editCover(manga: Manga, context: Context, data: Uri) {
        Observable
            .fromCallable {
                context.contentResolver.openInputStream(data)?.use {
                    if (manga.isLocal()) {
                        LocalSource.updateCover(context, manga, it)
                        manga.updateCoverLastModified(db)
                    } else if (manga.favorite) {
                        coverCache.setCustomCoverToCache(manga, it)
                        manga.updateCoverLastModified(db)
                    }
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ -> view.onSetCoverSuccess() },
                { view, e -> view.onSetCoverError(e) }
            )
    }

    fun deleteCustomCover(manga: Manga) {
        Observable
            .fromCallable {
                coverCache.deleteCustomCover(manga)
                manga.updateCoverLastModified(db)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ -> view.onSetCoverSuccess() },
                { view, e -> view.onSetCoverError(e) }
            )
    }

    // Manga info - end

    // Chapters list - start

    private fun observeDownloads() {
        observeDownloadsSubscription?.let { remove(it) }
        observeDownloadsSubscription = downloadManager.queue.getStatusObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .filter { download -> download.manga.id == manga.id }
            .doOnNext { onDownloadStatusChange(it) }
            .subscribeLatestCache(MangaController::onChapterStatusChange) { _, error ->
                Timber.e(error)
            }
    }

    /**
     * Converts a chapter from the database to an extended model, allowing to store new fields.
     */
    private fun Chapter.toModel(): ChapterItem {
        // Create the model object.
        val model = ChapterItem(this, manga)

        // Find an active download for this chapter.
        val download = downloadManager.queue.find { it.chapter.id == id }

        if (download != null) {
            // If there's an active download, assign it.
            model.download = download
        }
        return model
    }

    /**
     * Finds and assigns the list of downloaded chapters.
     *
     * @param chapters the list of chapter from the database.
     */
    private fun setDownloadedChapters(chapters: List<ChapterItem>) {
        chapters
            .filter { downloadManager.isChapterDownloaded(it, manga) }
            .forEach { it.status = Download.DOWNLOADED }
    }

    /**
     * Requests an updated list of chapters from the source.
     */
    fun fetchChaptersFromSource(manualFetch: Boolean = false) {
        hasRequested = true

        if (!fetchChaptersSubscription.isNullOrUnsubscribed()) return
        fetchChaptersSubscription = Observable.defer { source.fetchChapterList(manga) }
            .subscribeOn(Schedulers.io())
            .map { syncChaptersWithSource(db, it, manga, source) }
            .doOnNext {
                if (manualFetch) {
                    downloadNewChapters(it.first)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ ->
                    view.onFetchChaptersDone()
                },
                MangaController::onFetchChaptersError
            )
    }

    /**
     * Updates the UI after applying the filters.
     */
    private fun refreshChapters() {
        chaptersRelay.call(chapters)
    }

    /**
     * Applies the view filters to the list of chapters obtained from the database.
     * @param chapters the list of chapters from the database
     * @return an observable of the list of chapters filtered and sorted.
     */
    private fun applyChapterFilters(chapters: List<ChapterItem>): Observable<List<ChapterItem>> {
        var observable = Observable.from(chapters).subscribeOn(Schedulers.io())
        if (onlyUnread()) {
            observable = observable.filter { !it.read }
        } else if (onlyRead()) {
            observable = observable.filter { it.read }
        }
        if (onlyDownloaded()) {
            observable = observable.filter { it.isDownloaded || it.manga.isLocal() }
        }
        if (onlyBookmarked()) {
            observable = observable.filter { it.bookmark }
        }
        val sortFunction: (Chapter, Chapter) -> Int = when (manga.sorting) {
            Manga.SORTING_SOURCE -> when (sortDescending()) {
                true -> { c1, c2 -> c1.source_order.compareTo(c2.source_order) }
                false -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            }
            Manga.SORTING_NUMBER -> when (sortDescending()) {
                true -> { c1, c2 -> c2.chapter_number.compareTo(c1.chapter_number) }
                false -> { c1, c2 -> c1.chapter_number.compareTo(c2.chapter_number) }
            }
            Manga.SORTING_UPLOAD_DATE -> when (sortDescending()) {
                true -> { c1, c2 -> c2.date_upload.compareTo(c1.date_upload) }
                false -> { c1, c2 -> c1.date_upload.compareTo(c2.date_upload) }
            }
            else -> throw NotImplementedError("Unimplemented sorting method")
        }
        return observable.toSortedList(sortFunction)
    }

    /**
     * Called when a download for the active manga changes status.
     * @param download the download whose status changed.
     */
    private fun onDownloadStatusChange(download: Download) {
        // Assign the download to the model object.
        if (download.status == Download.QUEUE) {
            chapters.find { it.id == download.chapter.id }?.let {
                if (it.download == null) {
                    it.download = download
                }
            }
        }

        // Force UI update if downloaded filter active and download finished.
        if (onlyDownloaded() && download.status == Download.DOWNLOADED) {
            refreshChapters()
        }
    }

    /**
     * Returns the next unread chapter or null if everything is read.
     */
    fun getNextUnreadChapter(): ChapterItem? {
        return chapters.sortedByDescending { it.source_order }.find { !it.read }
    }

    /**
     * Mark the selected chapter list as read/unread.
     * @param selectedChapters the list of selected chapters.
     * @param read whether to mark chapters as read or unread.
     */
    fun markChaptersRead(selectedChapters: List<ChapterItem>, read: Boolean) {
        val chapters = selectedChapters.map { chapter ->
            chapter.read = read
            if (!read) {
                chapter.last_page_read = 0
            }
            chapter
        }

        launchIO {
            db.updateChaptersProgress(chapters).executeAsBlocking()

            if (preferences.removeAfterMarkedAsRead()) {
                deleteChapters(chapters)
            }
        }
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param chapters the list of chapters to download.
     */
    fun downloadChapters(chapters: List<Chapter>) {
        downloadManager.downloadChapters(manga, chapters)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param selectedChapters the list of chapters to bookmark.
     */
    fun bookmarkChapters(selectedChapters: List<ChapterItem>, bookmarked: Boolean) {
        Observable.from(selectedChapters)
            .doOnNext { chapter ->
                chapter.bookmark = bookmarked
            }
            .toList()
            .flatMap { db.updateChaptersProgress(it).asRxObservable() }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Deletes the given list of chapter.
     * @param chapters the list of chapters to delete.
     */
    fun deleteChapters(chapters: List<ChapterItem>) {
        Observable.just(chapters)
            .doOnNext { deleteChaptersInternal(chapters) }
            .doOnNext { if (onlyDownloaded()) refreshChapters() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ ->
                    view.onChaptersDeleted(chapters)
                },
                MangaController::onChaptersDeletedError
            )
    }

    private fun downloadNewChapters(chapters: List<Chapter>) {
        if (chapters.isEmpty() || !manga.shouldDownloadNewChapters(db, preferences)) return

        downloadChapters(chapters)
    }

    /**
     * Deletes a list of chapters from disk. This method is called in a background thread.
     * @param chapters the chapters to delete.
     */
    private fun deleteChaptersInternal(chapters: List<ChapterItem>) {
        downloadManager.deleteChapters(chapters, manga, source).forEach {
            if (it is ChapterItem) {
                it.status = Download.NOT_DOWNLOADED
                it.download = null
            }
        }
    }

    /**
     * Reverses the sorting and requests an UI update.
     */
    fun reverseSortOrder() {
        manga.setChapterOrder(if (sortDescending()) Manga.SORT_ASC else Manga.SORT_DESC)
        db.updateFlags(manga).executeAsBlocking()
        refreshChapters()
    }

    /**
     * Sets the read filter and requests an UI update.
     * @param onlyUnread whether to display only unread chapters or all chapters.
     */
    fun setUnreadFilter(onlyUnread: Boolean) {
        manga.readFilter = if (onlyUnread) Manga.SHOW_UNREAD else Manga.SHOW_ALL
        db.updateFlags(manga).executeAsBlocking()
        refreshChapters()
    }

    /**
     * Sets the read filter and requests an UI update.
     * @param onlyRead whether to display only read chapters or all chapters.
     */
    fun setReadFilter(onlyRead: Boolean) {
        manga.readFilter = if (onlyRead) Manga.SHOW_READ else Manga.SHOW_ALL
        db.updateFlags(manga).executeAsBlocking()
        refreshChapters()
    }

    /**
     * Sets the download filter and requests an UI update.
     * @param onlyDownloaded whether to display only downloaded chapters or all chapters.
     */
    fun setDownloadedFilter(onlyDownloaded: Boolean) {
        manga.downloadedFilter = if (onlyDownloaded) Manga.SHOW_DOWNLOADED else Manga.SHOW_ALL
        db.updateFlags(manga).executeAsBlocking()
        refreshChapters()
    }

    /**
     * Sets the bookmark filter and requests an UI update.
     * @param onlyBookmarked whether to display only bookmarked chapters or all chapters.
     */
    fun setBookmarkedFilter(onlyBookmarked: Boolean) {
        manga.bookmarkedFilter = if (onlyBookmarked) Manga.SHOW_BOOKMARKED else Manga.SHOW_ALL
        db.updateFlags(manga).executeAsBlocking()
        refreshChapters()
    }

    /**
     * Sets the active display mode.
     * @param mode the mode to set.
     */
    fun setDisplayMode(mode: Int) {
        manga.displayMode = mode
        db.updateFlags(manga).executeAsBlocking()
    }

    /**
     * Sets the sorting method and requests an UI update.
     * @param sort the sorting mode.
     */
    fun setSorting(sort: Int) {
        manga.sorting = sort
        db.updateFlags(manga).executeAsBlocking()
        refreshChapters()
    }

    /**
     * Whether downloaded only mode is enabled.
     */
    fun forceDownloaded(): Boolean {
        return manga.favorite && preferences.downloadedOnly().get()
    }

    /**
     * Whether the display only downloaded filter is enabled.
     */
    fun onlyDownloaded(): Boolean {
        return forceDownloaded() || manga.downloadedFilter == Manga.SHOW_DOWNLOADED
    }

    /**
     * Whether the display only downloaded filter is enabled.
     */
    fun onlyBookmarked(): Boolean {
        return manga.bookmarkedFilter == Manga.SHOW_BOOKMARKED
    }

    /**
     * Whether the display only unread filter is enabled.
     */
    fun onlyUnread(): Boolean {
        return manga.readFilter == Manga.SHOW_UNREAD
    }

    /**
     * Whether the display only read filter is enabled.
     */
    fun onlyRead(): Boolean {
        return manga.readFilter == Manga.SHOW_READ
    }

    /**
     * Whether the sorting method is descending or ascending.
     */
    fun sortDescending(): Boolean {
        return manga.sortDescending()
    }

    // Chapters list - end
}
