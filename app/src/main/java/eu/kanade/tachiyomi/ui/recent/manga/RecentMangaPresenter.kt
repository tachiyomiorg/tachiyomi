package eu.kanade.tachiyomi.ui.recent.manga

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * The id of the restartable.
 */
const private val GET_RECENT_MANGA = 1

class RecentMangaPresenter : BasePresenter<RecentMangaFragment>() {
    /**
     * Used to connect to database
     */
    @Inject lateinit var db: DatabaseHelper


    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Used to get recent manga
        restartableLatestCache(GET_RECENT_MANGA,
                { getRecentMangaObservable() },
                { recentChaptersFragment, chapters ->
                    // Update adapter to show recent manga's
                    recentChaptersFragment.onNextMangaChapters(chapters)
                }
        )

        if (savedState == null) {
            // Start fetching recent chapters
            start(GET_RECENT_MANGA)
        }
    }

    fun getRecentMangaObservable(): Observable<MutableList<Manga>>? {
        // Set date for recent chapters
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.MONTH, -1)

        return db.getRecentManga(cal.time).asRxObservable()
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun removeFromHistory(id: Long) {
        val history = db.getHistoryByMangaId(id).executeAsBlocking()
        history?.let {
            it.last_read = 0L
        }
        db.insertHistory(history!!).executeAsBlocking()
    }

    fun getNextUnreadChapter(manga: Manga): Chapter? {
        return db.getNextUnreadChapter(manga).executeAsBlocking()
    }

    fun getLastRead(id: Long): String? {
        val history = db.getHistoryByMangaId(id).executeAsBlocking()
        return SimpleDateFormat("dd-MM-yyyy HH:mm",
                java.util.Locale.getDefault()).format(Date(history?.last_read as Long));

    }

}