package eu.kanade.tachiyomi.ui.manga

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.manga.info.ChapterCountEvent
import eu.kanade.tachiyomi.util.SharedData
import rx.Observable
import rx.Subscription
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of [MangaActivity].
 */
class MangaPresenter : BasePresenter<MangaActivity>() {

    /**
     * Database helper.
     */
    val db: DatabaseHelper by injectLazy()

    /**
     * Tracking manager.
     */
    val trackManager: TrackManager by injectLazy()

    /**
     * Manga associated with this instance.
     */
    lateinit var manga: Manga

    var mangaSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Prepare a subject to communicate the chapters and info presenters for the chapter count.
        SharedData.put(ChapterCountEvent())
    }

    fun setMangaEvent(event: MangaEvent) {
        if (isUnsubscribed(mangaSubscription)) {
            manga = event.manga
            mangaSubscription = Observable.just(manga)
                    .subscribeLatestCache({ view, manga -> view.onSetManga(manga) })
        }
    }

}
