package eu.kanade.tachiyomi.ui.manga.chapter

import android.os.Bundle
import android.util.Pair
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.base.Source
import eu.kanade.tachiyomi.event.ChapterCountEvent
import eu.kanade.tachiyomi.event.DownloadChaptersEvent
import eu.kanade.tachiyomi.event.MangaEvent
import eu.kanade.tachiyomi.event.ReaderEvent
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.SharedData
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class ChaptersPresenter : BasePresenter<ChaptersFragment>() {

    @Inject lateinit var db: DatabaseHelper
    @Inject lateinit var sourceManager: SourceManager
    @Inject lateinit var preferences: PreferencesHelper
    @Inject lateinit var downloadManager: DownloadManager

    lateinit var manga: Manga
        private set

    lateinit var source: Source
        private set

    lateinit var chapters: List<Chapter>
        private set

    lateinit var chaptersSubject: PublishSubject<List<Chapter>>
        private set

    var hasRequested: Boolean = false
        private set

    private val DB_CHAPTERS = 1
    private val FETCH_CHAPTERS = 2
    private val CHAPTER_STATUS_CHANGES = 3

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        chaptersSubject = PublishSubject.create()

        startableLatestCache(DB_CHAPTERS,
                { getDbChaptersObs() },
                { view, chapters -> view.onNextChapters(chapters) })

        startableFirst(FETCH_CHAPTERS,
                { getOnlineChaptersObs() },
                { view, result -> view.onFetchChaptersDone() },
                { view, error -> view.onFetchChaptersError(error) })

        startableLatestCache(CHAPTER_STATUS_CHANGES,
                { getChapterStatusObs() },
                { view, download -> view.onChapterStatusChange(download) },
                { view, error -> Timber.e(error.cause, error.message) })

        manga = SharedData.get(MangaEvent::class.java)?.manga ?: return
        Observable.just(manga)
                .subscribeLatestCache({ view, manga -> view.onNextManga(manga) })

        source = sourceManager.get(manga.source)!!
        start(DB_CHAPTERS)

        add(db.getChapters(manga).asRxObservable()
                .subscribeOn(Schedulers.io())
                .doOnNext { chapters ->
                    this.chapters = chapters
                    SharedData.get(ChapterCountEvent::class.java)?.emit(chapters.size)
                    for (chapter in chapters) {
                        setChapterStatus(chapter)
                    }
                    start(CHAPTER_STATUS_CHANGES)
                }
                .subscribe { chaptersSubject.onNext(it) })
    }

    fun fetchChaptersFromSource() {
        hasRequested = true
        start(FETCH_CHAPTERS)
    }

    private fun refreshChapters() {
        chaptersSubject.onNext(chapters)
    }

    fun getOnlineChaptersObs(): Observable<Pair<Int, Int>> {
        return source.pullChaptersFromNetwork(manga.url)
                .subscribeOn(Schedulers.io())
                .flatMap { chapters -> db.insertOrRemoveChapters(manga, chapters, source) }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun getDbChaptersObs(): Observable<List<Chapter>> {
        return chaptersSubject
                .flatMap { applyChapterFilters(it) }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun getChapterStatusObs(): Observable<Download> {
        return downloadManager.queue.getStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .filter { download -> download.manga.id == manga.id }
                .doOnNext { updateChapterStatus(it) }
    }

    private fun applyChapterFilters(chapters: List<Chapter>): Observable<List<Chapter>> {
        var observable = Observable.from(chapters).subscribeOn(Schedulers.io())
        if (onlyUnread()) {
            observable = observable.filter { chapter -> !chapter.read }
        }
        if (onlyDownloaded()) {
            observable = observable.filter { chapter -> chapter.status == Download.DOWNLOADED }
        }
        return observable.toSortedList { chapter, chapter2 ->
            if (sortOrder())
                chapter2.chapter_number.compareTo(chapter.chapter_number)
            else
                chapter.chapter_number.compareTo(chapter2.chapter_number)
        }
    }

    private fun setChapterStatus(chapter: Chapter) {
        for (download in downloadManager.queue) {
            if (chapter.id == download.chapter.id) {
                chapter.status = download.status
                return
            }
        }

        if (downloadManager.isChapterDownloaded(source, manga, chapter)) {
            chapter.status = Download.DOWNLOADED
        } else {
            chapter.status = Download.NOT_DOWNLOADED
        }
    }

    fun updateChapterStatus(download: Download) {
        for (chapter in chapters) {
            if (download.chapter.id == chapter.id) {
                chapter.status = download.status
                break
            }
        }
        if (onlyDownloaded() && download.status == Download.DOWNLOADED)
            refreshChapters()
    }

    fun onOpenChapter(chapter: Chapter) {
        SharedData.put(ReaderEvent(manga, chapter))
    }

    fun getNextUnreadChapter(): Chapter? {
        return db.getNextUnreadChapter(manga).executeAsBlocking()
    }

    fun markChaptersRead(selectedChapters: Observable<Chapter>, read: Boolean) {
        add(selectedChapters.subscribeOn(Schedulers.io())
                .doOnNext { chapter ->
                    chapter.read = read
                    if (!read) chapter.last_page_read = 0

                    // Delete chapter when marked as read if desired by user.
                    if (preferences.removeAfterMarkedAsRead() && read) {
                        deleteChapter(chapter)
                    }
                }
                .toList()
                .flatMap { chapters -> db.insertChapters(chapters).asRxObservable() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe())
    }

    fun markPreviousChaptersAsRead(selected: Chapter) {
        Observable.from(chapters)
                .filter { c -> c.chapter_number > -1 && c.chapter_number < selected.chapter_number }
                .doOnNext { c -> c.read = true }
                .toList()
                .flatMap { chapters -> db.insertChapters(chapters).asRxObservable() }
                .subscribe()
    }

    fun downloadChapters(selectedChapters: Observable<Chapter>) {
        add(selectedChapters.toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { downloadManager.onDownloadChaptersEvent(DownloadChaptersEvent(manga, it)) })
    }

    fun deleteChapters(selectedChapters: Observable<Chapter>) {
        add(selectedChapters.subscribe(
                { chapter -> downloadManager.queue.del(chapter) },
                { error -> Timber.e(error.message) },
                {
                    if (onlyDownloaded())
                        refreshChapters()
                }))
    }

    fun deleteChapter(chapter: Chapter) {
        downloadManager.deleteChapter(source, manga, chapter)
    }

    fun revertSortOrder() {
        manga.setChapterOrder(if (sortOrder()) Manga.SORT_ZA else Manga.SORT_AZ)
        db.insertManga(manga).executeAsBlocking()
        refreshChapters()
    }

    fun setReadFilter(onlyUnread: Boolean) {
        manga.readFilter = if (onlyUnread) Manga.SHOW_UNREAD else Manga.SHOW_ALL
        db.insertManga(manga).executeAsBlocking()
        refreshChapters()
    }

    fun setDownloadedFilter(onlyDownloaded: Boolean) {
        manga.downloadedFilter = if (onlyDownloaded) Manga.SHOW_DOWNLOADED else Manga.SHOW_ALL
        db.insertManga(manga).executeAsBlocking()
        refreshChapters()
    }

    fun setDisplayMode(mode: Int) {
        manga.displayMode = mode
        db.insertManga(manga).executeAsBlocking()
    }

    fun onlyDownloaded(): Boolean {
        return manga.downloadedFilter == Manga.SHOW_DOWNLOADED
    }

    fun onlyUnread(): Boolean {
        return manga.readFilter == Manga.SHOW_UNREAD
    }

    fun sortOrder(): Boolean {
        return manga.sortChaptersAZ()
    }

}
