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
import java.util.Calendar
import java.util.Comparator
import java.util.Date
import java.util.TreeMap
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.injectLazy

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

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Used to get a list of recently read manga
        getRecentMangaObservable()
            .subscribeLatestCache(HistoryController::onNextManga)
    }

    /**
     * Get recent manga observable
     * @return list of history
     */
    fun getRecentMangaObservable(): Observable<List<HistoryItem>> {
        // Set date limit for recent manga
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.MONTH, -3)
        }

        return db.getRecentManga(cal.time).asRxObservable()
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
            Manga.SORTING_SOURCE -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            Manga.SORTING_NUMBER -> { c1, c2 -> c1.chapter_number.compareTo(c2.chapter_number) }
            Manga.SORTING_UPLOAD_DATE -> { c1, c2 -> c1.date_upload.compareTo(c2.date_upload) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        val chapters = db.getChapters(manga).executeAsBlocking()
            .sortedWith(Comparator { c1, c2 -> sortFunction(c1, c2) })

        val currChapterIndex = chapters.indexOfFirst { chapter.id == it.id }
        return when (manga.sorting) {
            Manga.SORTING_SOURCE -> chapters.getOrNull(currChapterIndex + 1)
            Manga.SORTING_NUMBER -> {
                val chapterNumber = chapter.chapter_number

                ((currChapterIndex + 1) until chapters.size)
                    .map { chapters[it] }
                    .firstOrNull {
                        it.chapter_number > chapterNumber &&
                            it.chapter_number <= chapterNumber + 1
                    }
            }
            Manga.SORTING_UPLOAD_DATE -> {
                chapters.drop(currChapterIndex + 1)
                    .firstOrNull { it.date_upload >= chapter.date_upload }
            }
            else -> throw NotImplementedError("Unknown sorting method")
        }
    }
}
