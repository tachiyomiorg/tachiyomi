package eu.kanade.tachiyomi.ui.manga

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.event.ChapterCountEvent
import eu.kanade.tachiyomi.event.MangaEvent
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.SharedData
import rx.Observable
import javax.inject.Inject

/**
 * Presenter of [MangaActivity].
 */
class MangaPresenter : BasePresenter<MangaActivity>() {

    /**
     * Database helper.
     */
    @Inject lateinit var db: DatabaseHelper

    /**
     * Manga sync manager.
     */
    @Inject lateinit var syncManager: MangaSyncManager

    /**
     * Manga associated with this instance.
     */
    lateinit var manga: Manga

    /**
     * Key to save and restore [manga] from a bundle.
     */
    private val MANGA_KEY = "manga_key"

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (savedState == null) {
            manga = SharedData.get(MangaEvent::class.java)!!.manga
        } else {
            manga = savedState.getSerializable(MANGA_KEY) as Manga
            SharedData.put(MangaEvent(manga))
        }

        // Prepare a subject to communicate the chapters and info presenters for the chapter count.
        SharedData.put(ChapterCountEvent())

        Observable.just(manga)
                .subscribeLatestCache({ view, manga -> view.onSetManga(manga) })
    }

    override fun onDestroy() {
        SharedData.remove(MangaEvent::class.java)
        SharedData.remove(ChapterCountEvent::class.java)
        super.onDestroy()
    }

    override fun onSave(state: Bundle) {
        state.putSerializable(MANGA_KEY, manga)
        super.onSave(state)
    }

}
