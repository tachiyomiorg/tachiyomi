package eu.kanade.tachiyomi.ui.recently_read

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * The id of the restartable.
 */
const private val GET_RECENT_MANGA = 1

/**
 * Presenter of RecentlyReadFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class RecentlyReadPresenter : BasePresenter<RecentlyReadFragment>() {
    /**
     * Used to connect to database
     */
    @Inject lateinit var db: DatabaseHelper

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Used to get recent manga
        restartableLatestCache(GET_RECENT_MANGA,
                { getRecentMangaObservable() },
                { recentMangaFragment, manga ->
                    // Update adapter to show recent manga's
                    recentMangaFragment.onNextManga(manga)
                }
        )

        if (savedState == null) {
            // Start fetching recent manga
            start(GET_RECENT_MANGA)
        }
    }

    /**
     * Get recent manga observable
     * @return list of history
     */
    fun getRecentMangaObservable(): Observable<MutableList<MangaChapterHistory>> {
        // Set date for recent manga
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.MONTH, -1)

        return db.getRecentManga(cal.time).asRxObservable()
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Reset last read of chapter to 0L
     * @param history history belonging to chapter
     */
    fun removeFromHistory(history: History) {
        history.last_read = 0L
        db.updateHistoryLastRead(history).asRxObservable()
                .doOnError { Timber.e(it.message) }.subscribe()
    }

    /**
     * Removes all chapters belonging to manga from library
     * @param mangaId id of manga
     */
    fun removeAllFromHistory(mangaId: Long) {
        db.getHistoryByMangaId(mangaId).asRxObservable()
                .take(1)
                .flatMapIterable { it }
                .doOnError { Timber.e(it.message) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result -> removeFromHistory(result) })
    }

    /**
     * Returns the timestamp of last read
     * @param history history containing time of last read
     */
    fun getLastRead(history: History): String? {
        return SimpleDateFormat("dd-MM-yyyy HH:mm",
                Locale.getDefault()).format(Date(history.last_read))
    }

}
