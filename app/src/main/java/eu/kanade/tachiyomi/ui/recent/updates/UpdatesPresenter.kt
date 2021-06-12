package eu.kanade.tachiyomi.ui.recent.updates

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.recent.DateSectionItem
import eu.kanade.tachiyomi.util.lang.toDateKey
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import java.util.Date
import java.util.TreeMap

class UpdatesPresenter(
    val preferences: PreferencesHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get()
) : BasePresenter<UpdatesController>() {

    /**
     * List containing chapter and manga information
     */
    private var chapters: List<UpdatesItem> = emptyList()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        getUpdatesObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(UpdatesController::onNextRecentChapters)

        downloadManager.queue.getStatusObservable()
            .observeOn(Schedulers.io())
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                { view, it ->
                    onDownloadStatusChange(it)
                    view.onChapterDownloadUpdate(it)
                },
                { _, error ->
                    Timber.e(error)
                }
            )

        downloadManager.queue.getProgressObservable()
            .observeOn(Schedulers.io())
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(UpdatesController::onChapterDownloadUpdate) { _, error ->
                Timber.e(error)
            }
    }

    /**
     * Get observable containing recent chapters and date
     *
     * @return observable containing recent chapters and date
     */
    private fun getUpdatesObservable(): Observable<List<UpdatesItem>> {
        // Set date limit for recent chapters
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.MONTH, -3)
        }

        return db.getRecentChapters(cal.time).asRxObservable()
            // Convert to a list of recent chapters.
            .map { mangaChapters ->
                val map = TreeMap<Date, MutableList<MangaChapter>> { d1, d2 -> d2.compareTo(d1) }
                val byDay = mangaChapters
                    .groupByTo(map, { it.chapter.date_fetch.toDateKey() })
                byDay.flatMap { entry ->
                    val dateItem = DateSectionItem(entry.key)
                    entry.value
                        .sortedWith(compareBy({ it.chapter.date_fetch }, { it.chapter.chapter_number })).asReversed()
                        .map { UpdatesItem(it.chapter, it.manga, dateItem) }
                }
            }
            .doOnNext { list ->
                list.forEach { item ->
                    // Find an active download for this chapter.
                    val download = downloadManager.queue.find { it.chapter.id == item.chapter.id }

                    // If there's an active download, assign it, otherwise ask the manager if
                    // the chapter is downloaded and assign it to the status.
                    if (download != null) {
                        item.download = download
                    }
                }
                setDownloadedChapters(list)
                chapters = list
            }
    }

    /**
     * Finds and assigns the list of downloaded chapters.
     *
     * @param items the list of chapter from the database.
     */
    private fun setDownloadedChapters(items: List<UpdatesItem>) {
        for (item in items) {
            val manga = item.manga
            val chapter = item.chapter

            if (downloadManager.isChapterDownloaded(chapter, manga)) {
                item.status = Download.State.DOWNLOADED
            }
        }
    }

    /**
     * Update status of chapters.
     *
     * @param download download object containing progress.
     */
    private fun onDownloadStatusChange(download: Download) {
        // Assign the download to the model object.
        if (download.status == Download.State.QUEUE) {
            val chapter = chapters.find { it.chapter.id == download.chapter.id }
            if (chapter != null && chapter.download == null) {
                chapter.download = download
            }
        }
    }

    fun startDownloadingNow(chapter: Chapter) {
        downloadManager.startDownloadNow(chapter)
    }

    /**
     * Mark selected chapter as read
     *
     * @param items list of selected chapters
     * @param read read status
     */
    fun markChapterRead(items: List<UpdatesItem>, read: Boolean) {
        val chapters = items.map { it.chapter }
        chapters.forEach {
            it.read = read
            if (!read) {
                it.last_page_read = 0
            }
        }

        Observable.fromCallable { db.updateChaptersProgress(chapters).executeAsBlocking() }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Delete selected chapters
     *
     * @param chapters list of chapters
     */
    fun deleteChapters(chapters: List<UpdatesItem>) {
        Observable.just(chapters)
            .doOnNext { deleteChaptersInternal(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ ->
                    view.onChaptersDeleted()
                },
                UpdatesController::onChaptersDeletedError
            )
    }

    /**
     * Download selected chapters
     * @param items list of recent chapters seleted.
     */
    fun downloadChapters(items: List<UpdatesItem>) {
        items.forEach { downloadManager.downloadChapters(it.manga, listOf(it.chapter)) }
    }

    /**
     * Delete selected chapters
     *
     * @param items chapters selected
     */
    private fun deleteChaptersInternal(chapterItems: List<UpdatesItem>) {
        val itemsByManga = chapterItems.groupBy { it.manga.id }
        for ((_, items) in itemsByManga) {
            val manga = items.first().manga
            val source = sourceManager.get(manga.source) ?: continue
            val chapters = items.map { it.chapter }

            downloadManager.deleteChapters(chapters, manga, source)
            items.forEach {
                it.status = Download.State.NOT_DOWNLOADED
                it.download = null
            }
        }
    }
}
