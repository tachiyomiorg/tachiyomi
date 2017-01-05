package eu.kanade.tachiyomi.ui.manga.info

import com.jakewharton.rxrelay.PublishRelay
import rx.Observable

class MangaFavoriteEvent {

    private val subject = PublishRelay.create<Boolean>()

    val observable: Observable<Boolean>
        get() = subject

    fun call(favorite: Boolean) {
        subject.call(favorite)
    }
}
