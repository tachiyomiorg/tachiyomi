package eu.kanade.tachiyomi.ui.manga

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.event.MangaEvent
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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

    /**
     * Id of the restartable that notifies the view of a manga.
     */
    private val GET_MANGA = 1

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (savedState != null) {
            manga = savedState.getSerializable(MANGA_KEY) as Manga
        }

        restartableLatestCache(GET_MANGA,
                { Observable.just(manga)
                        .doOnNext { EventBus.getDefault().postSticky(MangaEvent(it)) } },
                { view, manga -> view.onSetManga(manga) })

        if (savedState == null) {
            registerForEvents()
        }
    }

    override fun onDestroy() {
        // Avoid new instances receiving wrong manga
        EventBus.getDefault().removeStickyEvent(MangaEvent::class.java)

        super.onDestroy()
    }

    override fun onSave(state: Bundle) {
        state.putSerializable(MANGA_KEY, manga)
        super.onSave(state)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(manga: Manga) {
        EventBus.getDefault().removeStickyEvent(manga)
        unregisterForEvents()
        this.manga = manga
        start(GET_MANGA)
    }

}
