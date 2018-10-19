package eu.kanade.tachiyomi.ui.manga.chapter

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.isNullOrUnsubscribed
import eu.kanade.tachiyomi.util.syncChaptersWithSource
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

/**
 * Presenter of [ChaptersController].
 */
class ChaptersPresenter(
        val manga: Manga,
        val source: Source,
        private val chapterCountRelay: BehaviorRelay<Float>,
        private val lastUpdateRelay: BehaviorRelay<Date>,
        private val mangaFavoriteRelay: PublishRelay<Boolean>,
        val preferences: PreferencesHelper = Injekt.get(),
        private val db: DatabaseHelper = Injekt.get(),
        private val downloadManager: DownloadManager = Injekt.get()
) : BasePresenter<ChaptersController>() {

    /**
     * List of chapters of the manga. It's always unfiltered and unsorted.
     */
    var chapters: List<ChapterItem> = emptyList()
        private set

    /**
     * Subject of list of chapters to allow updating the view without going to DB.
     */
    val chaptersRelay: PublishRelay<List<ChapterItem>>
            by lazy { PublishRelay.create<List<ChapterItem>>() }

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

        // Prepare the relay.
        chaptersRelay.flatMap { applyChapterFilters(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(ChaptersController::onNextChapters,
                        { _, error -> Timber.e(error) })

        // Add the subscription that retrieves the chapters from the database, keeps subscribed to
        // changes, and sends the list of chapters to the relay.
        add(db.getChapters(manga).asRxObservable()
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

                    // Emit the number of chapters to the info tab.
                    chapterCountRelay.call(chapters.maxBy { it.chapter_number }?.chapter_number
                            ?: 0f)

                    // Emit the upload date of the most recent chapter
                    lastUpdateRelay.call(Date(chapters.maxBy { it.date_upload }?.date_upload
                            ?: 0))

                }
                .subscribe { chaptersRelay.call(it) })
    }

    private fun observeDownloads() {
        observeDownloadsSubscription?.let { remove(it) }
        observeDownloadsSubscription = downloadManager.queue.getStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .filter { download -> download.manga.id == manga.id }
                .doOnNext { onDownloadStatusChange(it) }
                .subscribeLatestCache(ChaptersController::onChapterStatusChange,
                        { _, error -> Timber.e(error) })
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
        for (chapter in chapters) {
            if (downloadManager.isChapterDownloaded(chapter, manga)) {
                chapter.status = Download.DOWNLOADED
            }
        }
    }

    /**
     * Requests an updated list of chapters from the source.
     */
    fun fetchChaptersFromSource() {
        hasRequested = true

        if (!fetchChaptersSubscription.isNullOrUnsubscribed()) return
        fetchChaptersSubscription = Observable.defer { source.fetchChapterList(manga) }
                .subscribeOn(Schedulers.io())
                .map { syncChaptersWithSource(db, it, manga, source) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst({ view, _ ->
                    view.onFetchChaptersDone()
                }, ChaptersController::onFetchChaptersError)
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
        }
        else if (onlyRead()) {
            observable = observable.filter { it.read }
        }
        if (onlyDownloaded()) {
            observable = observable.filter { it.isDownloaded || it.manga.source == LocalSource.ID }
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
            else -> throw NotImplementedError("Unimplemented sorting method")
        }
        return observable.toSortedList(sortFunction)
    }

    /**
     * Called when a download for the active manga changes status.
     * @param download the download whose status changed.
     */
    fun onDownloadStatusChange(download: Download) {
        // Assign the download to the model object.
        if (download.status == Download.QUEUE) {
            chapters.find { it.id == download.chapter.id }?.let {
                if (it.download == null) {
                    it.download = download
                }
            }
        }

        // Force UI update if downloaded filter active and download finished.
        if (onlyDownloaded() && download.status == Download.DOWNLOADED)
            refreshChapters()
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
        Observable.from(selectedChapters)
                .doOnNext { chapter ->
                    chapter.read = read
                    if (!read) {
                        chapter.last_page_read = 0
                    }
                }
                .toList()
                .flatMap { db.updateChaptersProgress(it).asRxObservable() }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param chapters the list of chapters to download.
     */
    fun downloadChapters(chapters: List<ChapterItem>) {
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
                .subscribeFirst({ view, _ ->
                    view.onChaptersDeleted()
                }, ChaptersController::onChaptersDeletedError)
    }

    /**
     * Deletes a list of chapters from disk. This method is called in a background thread.
     * @param chapters the chapters to delete.
     */
    private fun deleteChaptersInternal(chapters: List<ChapterItem>) {
        downloadManager.deleteChapters(chapters, manga, source)
        chapters.forEach {
            it.status = Download.NOT_DOWNLOADED
            it.download = null
        }
    }

    /**
     * Reverses the sorting and requests an UI update.
     */
    fun revertSortOrder() {
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
     * Removes all filters and requests an UI update.
     */
    fun removeFilters() {
        manga.readFilter = Manga.SHOW_ALL
        manga.downloadedFilter = Manga.SHOW_ALL
        manga.bookmarkedFilter = Manga.SHOW_ALL
        db.updateFlags(manga).executeAsBlocking()
        refreshChapters()
    }

    /**
     * Adds manga to library
     */
    fun addToLibrary() {
        mangaFavoriteRelay.call(true)
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
     * Whether the display only downloaded filter is enabled.
     */
    fun onlyDownloaded(): Boolean {
        return manga.downloadedFilter == Manga.SHOW_DOWNLOADED
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

}
