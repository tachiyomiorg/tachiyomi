package eu.kanade.tachiyomi.ui.recently_read

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.util.*

/**
 * Presenter of RecentlyReadFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class RecentlyReadPresenter : BasePresenter<RecentlyReadController>() {

    /**
     * Used to connect to database
     */
    val db: DatabaseHelper by injectLazy()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Used to get a list of recently read manga
        getRecentMangaObservable()
                .subscribeLatestCache(RecentlyReadController::onNextManga)
    }

    /**
     * Get recent manga observable
     * @return list of history
     */
    fun getRecentMangaObservable(): Observable<List<RecentlyReadItem>> {
        // Set date for recent manga
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.MONTH, -1)

        return db.getRecentManga(cal.time).asRxObservable()
                .map { it.map(::RecentlyReadItem) }
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
     * Open the next chapter instead of the current one.
     * @param chapter the chapter of the history object.
     * @param manga the manga of the chapter.
     */
    fun openNextChapter(chapter: Chapter, manga: Manga) {
        val sortFunction: (Chapter, Chapter) -> Int = when (manga.sorting) {
            Manga.SORTING_SOURCE -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            Manga.SORTING_NUMBER -> { c1, c2 -> c1.chapter_number.compareTo(c2.chapter_number) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        db.getChapters(manga).asRxSingle()
                .map { it.sortedWith(Comparator<Chapter> { c1, c2 -> sortFunction(c1, c2) }) }
                .map { chapters ->
                    val currChapterIndex = chapters.indexOfFirst { chapter.id == it.id }
                    when (manga.sorting) {
                        Manga.SORTING_SOURCE -> {
                            chapters.getOrNull(currChapterIndex + 1)
                        }
                        Manga.SORTING_NUMBER -> {
                            val chapterNumber = chapter.chapter_number

                            var nextChapter: Chapter? = null
                            for (i in (currChapterIndex + 1) until chapters.size) {
                                val c = chapters[i]
                                if (c.chapter_number > chapterNumber &&
                                        c.chapter_number <= chapterNumber + 1) {

                                    nextChapter = c
                                    break
                                }
                            }
                            nextChapter
                        }
                        else -> throw NotImplementedError("Unknown sorting method")
                    }
                }
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst({ view, chapter ->
                    view.onOpenNextChapter(chapter, manga)
                }, { _, error ->
                    Timber.e(error)
                })
    }

}
