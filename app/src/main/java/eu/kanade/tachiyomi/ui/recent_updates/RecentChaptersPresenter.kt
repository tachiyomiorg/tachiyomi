package eu.kanade.tachiyomi.ui.recent_updates

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class RecentChaptersPresenter : BasePresenter<RecentChaptersFragment>() {
    /**
     * Used to connect to database
     */
    @Inject lateinit var db: DatabaseHelper

    /**
     * Used to get settings
     */
    @Inject lateinit var preferences: PreferencesHelper

    /**
     * Used to get information from download manager
     */
    @Inject lateinit var downloadManager: DownloadManager

    /**
     * Used to get source from source id
     */
    @Inject lateinit var sourceManager: SourceManager

    /**
     * List containing chapter and manga information
     */
    private var mangaChapters: List<MangaChapter>? = null

    /**
     * The id of the restartable.
     */
    val GET_RECENT_CHAPTERS = 1

    /**
     * The id of the restartable.
     */
    val CHAPTER_STATUS_CHANGES = 2

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Used to get recent chapters
        restartableLatestCache(GET_RECENT_CHAPTERS,
                { getRecentChaptersObservable() },
                { fragment, chapters ->
                    // Update adapter to show recent manga's
                    fragment.onNextMangaChapters(chapters)
                    // Update download status
                    updateChapterStatus(convertToMangaChaptersList(chapters))
                }
        )

        // Used to update download status
        startableLatestCache(CHAPTER_STATUS_CHANGES,
                { getChapterStatusObs() },
                { recentChaptersFragment, download ->
                    // Set chapter status
                    recentChaptersFragment.onChapterStatusChange(download)
                },
                { view, error -> Timber.e(error.cause, error.message) }
        )


        if (savedState == null) {
            // Start fetching recent chapters
            start(GET_RECENT_CHAPTERS)
        }
    }


    /**
     * Returns observable containing chapter status.
     * @return download object containing download progress.
     */
    private fun getChapterStatusObs(): Observable<Download> {
        return downloadManager.queue.getStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .filter { download: Download ->
                    if (chapterIdEquals(download.chapter.id))
                        true
                    else
                        false
                }
                .doOnNext { download1: Download -> updateChapterStatus(download1) }

    }

    /**
     * Function to check if chapter is in recent list
     * @param chaptersId id of chapter
     * @return exist in recent list
     */
    private fun chapterIdEquals(chaptersId: Long): Boolean {
        mangaChapters!!.forEach { mangaChapter ->
            if (chaptersId == mangaChapter.chapter.id) {
                return true
            }
        }
        return false
    }

    /**
     * Returns a list only containing MangaChapter objects.
     * @param input the list that will be converted.
     * @return list containing MangaChapters objects.
     */
    private fun convertToMangaChaptersList(input: List<Any>): List<MangaChapter> {
        // Create temp list
        val tempMangaChapterList = ArrayList<MangaChapter>()

        // Only add MangaChapter objects
        //noinspection Convert2streamapi
        input.forEach { `object` ->
            if (`object` is MangaChapter) {
                tempMangaChapterList.add(`object`)
            }
        }

        // Return temp list
        return tempMangaChapterList
    }

    /**
     * Update status of chapters.
     * @param download download object containing progress.
     */
    private fun updateChapterStatus(download: Download) {
        // Loop through list
        mangaChapters?.let {
            for (item in it) {
                if (download.chapter.id == item.chapter.id) {
                    // Update status.
                    item.chapter.status = download.status
                    break
                }
            }
        }
    }

    /**
     * Update status of chapters
     * @param mangaChapters list containing recent chapters
     */
    private fun updateChapterStatus(mangaChapters: List<MangaChapter>) {
        // Set global list of chapters.
        this.mangaChapters = mangaChapters

        // Update status.
        //noinspection Convert2streamapi
        for (mangaChapter in mangaChapters)
            setChapterStatus(mangaChapter)

        // Start onChapterStatusChange restartable.
        start(CHAPTER_STATUS_CHANGES)
    }

    /**
     * Set the chapter status
     * @param mangaChapter MangaChapter which status gets updated
     */
    private fun setChapterStatus(mangaChapter: MangaChapter) {
        // Check if chapter in queue
        for (download in downloadManager.queue) {
            if (mangaChapter.chapter.id == download.chapter.id) {
                mangaChapter.chapter.status = download.status
                return
            }
        }

        // Get source of chapter
        val source = sourceManager.get(mangaChapter.manga.source)!!

        // Check if chapter is downloaded
        if (downloadManager.isChapterDownloaded(source, mangaChapter.manga, mangaChapter.chapter)) {
            mangaChapter.chapter.status = Download.DOWNLOADED
        } else {
            mangaChapter.chapter.status = Download.NOT_DOWNLOADED
        }
    }

    /**
     * Get observable containing recent chapters and date
     * @return observable containing recent chapters and date
     */
    fun getRecentChaptersObservable(): Observable<ArrayList<Any>>? {
        // Set date for recent chapters
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.MONTH, -1)

        return db.getRecentChapters(cal.time).asRxObservable()
                // Group chapters by the date they were fetched on a ordered map.
                .flatMap { recentItems ->
                    Observable.from(recentItems)
                            .toMultimap(
                                    { getMapKey(it.chapter.date_fetch) },
                                    { it },
                                    { TreeMap { d1, d2 -> d2.compareTo(d1) } })
                }
                // Add every day and all its chapters to a single list.
                .map { recentItems ->
                    val items = ArrayList<Any>()
                    recentItems.entries.forEach { recent ->
                        items.add(recent.key)
                        items.addAll(recent.value)
                    }
                    items
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Get date as time key
     * @param date desired date
     * @return date as time key
     */
    private fun getMapKey(date: Long): Date {
        val cal = Calendar.getInstance()
        cal.time = Date(date)
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal.time
    }

    /**
     * Mark selected chapter as read
     * @param mangaChapters list of selected MangaChapters
     * @param read read status
     */
    fun markChapterRead(mangaChapters: List<MangaChapter>, read: Boolean) {
        Observable.from(mangaChapters)
                .doOnNext { mangaChapter ->
                    mangaChapter.chapter.read = read
                    if (!read) {
                        mangaChapter.chapter.last_page_read = 0
                    }
                }
                .map { mangaChapter -> mangaChapter.chapter }
                .toList()
                .flatMap { db.updateChaptersProgress(it).asRxObservable() }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    /**
     * Delete selected chapters
     * @param chapters list of MangaChapters
     */
    fun deleteChapters(chapters: List<MangaChapter>) {
        val wasRunning = downloadManager.isRunning
        if (wasRunning) {
            DownloadService.stop(context)
        }
        Observable.from(chapters)
                .doOnNext { deleteChapter(it) }
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst({ view, result ->
                    view.onChaptersDeleted()
                    if (wasRunning) {
                        DownloadService.start(context)
                    }
                }, { view, error ->
                    view.onChaptersDeletedError(error)
                })
    }

    /**
     * Download selected chapters
     * @param mangaChapters [MangaChapter] that is selected
     */
    fun downloadChapters(mangaChapters: List<MangaChapter>) {
        DownloadService.start(context)
        Observable.from(mangaChapters)
                .doOnNext { downloadChapter(it) }
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

    /**
     * Download selected chapter
     * @param item chapter that is selected
     */
    fun downloadChapter(item: MangaChapter) {
        DownloadService.start(context)
        downloadManager.downloadChapters(item.manga, listOf(item.chapter))
    }

    /**
     * Delete selected chapter
     * @param item chapter that are selected
     */
    fun deleteChapter(item: MangaChapter) {
        val wasRunning = downloadManager.isRunning
        if (wasRunning) {
            DownloadService.stop(context)
        }
        Observable.just(item)
                .doOnNext { deleteChapter(it.chapter, it.manga) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst({ view, result ->
                    view.onChaptersDeleted()
                    if (wasRunning) {
                        DownloadService.start(context)
                    }
                }, { view, error ->
                    view.onChaptersDeletedError(error)
                })
    }

    /**
     * Delete selected chapter
     * @param chapter chapter that is selected
     * @param manga manga that belongs to chapter
     */
    private fun deleteChapter(chapter: Chapter, manga: Manga) {
        val source = sourceManager.get(manga.source) ?: return
        downloadManager.queue.del(chapter)
        downloadManager.deleteChapter(source, manga, chapter)
        chapter.status = Download.NOT_DOWNLOADED
    }


}