package eu.kanade.tachiyomi.ui.recent.history

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.recent.DateSectionItem
import eu.kanade.tachiyomi.util.lang.toDateKey
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.injectLazy
import java.util.Calendar
import java.util.Date
import java.util.TreeMap

/**
 * Presenter of HistoryFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class HistoryPresenter : BasePresenter<HistoryController>() {

    /**
     * Used to connect to database
     */
    val db: DatabaseHelper by injectLazy()

    private var recentMangaSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Used to get a list of recently read manga
        updateList()
    }

    fun requestNext(offset: Int, search: String = "") {
        getRecentMangaObservable(offset = offset, search = search)
            .subscribeLatestCache(
                { view, mangas ->
                    view.onNextManga(mangas)
                },
                HistoryController::onAddPageError
            )
    }

    /**
     * Get recent manga observable
     * @return list of history
     */
    private fun getRecentMangaObservable(limit: Int = 25, offset: Int = 0, search: String = ""): Observable<List<HistoryItem>> {
        // Set date limit for recent manga
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.YEAR, -50)
        }

        return db.getRecentManga(cal.time, limit, offset, search).asRxObservable()
            .map { recents ->
                val map = TreeMap<Date, MutableList<MangaChapterHistory>> { d1, d2 -> d2.compareTo(d1) }
                val byDay = recents
                    .groupByTo(map, { it.history.last_read.toDateKey() })
                byDay.flatMap { entry ->
                    val dateItem = DateSectionItem(entry.key)
                    entry.value.map { HistoryItem(it, dateItem) }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Reset last read of chapter to 0L
     * @param history history belonging to chapter
     */
    fun removeFromHistory(history: History) {
        history.last_read = 0L
        db.updateHistoryLastRead(history).asRxObservable()
            .subscribe()
    }

    /**
     * Pull a list of history from the db
     * @param search a search query to use for filtering
     */
    fun updateList(search: String = "") {
        recentMangaSubscription?.unsubscribe()
        recentMangaSubscription = getRecentMangaObservable(search = search)
            .subscribeLatestCache(
                { view, mangas ->
                    view.onNextManga(mangas, true)
                },
                HistoryController::onAddPageError
            )
    }

    /**
     * Removes all chapters belonging to manga from history.
     * @param mangaId id of manga
     */
    fun removeAllFromHistory(mangaId: Long) {
        db.getHistoryByMangaId(mangaId).asRxSingle()
            .map { list ->
                list.forEach { it.last_read = 0L }
                db.updateHistoryLastRead(list).executeAsBlocking()
            }
            .subscribe()
    }

    /**
     * Retrieves the next chapter of the given one.
     *
     * @param chapter the chapter of the history object.
     * @param manga the manga of the chapter.
     */
    fun getNextChapter(chapter: Chapter, manga: Manga): Chapter? {
        if (!chapter.read) {
            return chapter
        }

        val sortFunction: (Chapter, Chapter) -> Int = when (manga.sorting) {
            Manga.CHAPTER_SORTING_SOURCE -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            Manga.CHAPTER_SORTING_NUMBER -> { c1, c2 -> c1.chapter_number.compareTo(c2.chapter_number) }
            Manga.CHAPTER_SORTING_UPLOAD_DATE -> { c1, c2 -> c1.date_upload.compareTo(c2.date_upload) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        val chapters = db.getChapters(manga).executeAsBlocking()
            .sortedWith { c1, c2 -> sortFunction(c1, c2) }

        val currChapterIndex = chapters.indexOfFirst { chapter.id == it.id }
        return when (manga.sorting) {
            Manga.CHAPTER_SORTING_SOURCE -> chapters.getOrNull(currChapterIndex + 1)
            Manga.CHAPTER_SORTING_NUMBER -> {
                val chapterNumber = chapter.chapter_number

                ((currChapterIndex + 1) until chapters.size)
                    .map { chapters[it] }
                    .firstOrNull {
                        it.chapter_number > chapterNumber &&
                            it.chapter_number <= chapterNumber + 1
                    }
            }
            Manga.CHAPTER_SORTING_UPLOAD_DATE -> {
                chapters.drop(currChapterIndex + 1)
                    .firstOrNull { it.date_upload >= chapter.date_upload }
            }
            else -> throw NotImplementedError("Unknown sorting method")
        }
    }
}
