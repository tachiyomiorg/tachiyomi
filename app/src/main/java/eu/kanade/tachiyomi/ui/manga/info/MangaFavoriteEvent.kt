package eu.kanade.tachiyomi.ui.manga.info

import rx.Observable
import rx.subjects.BehaviorSubject

class MangaFavoriteEvent {

    private val subject = BehaviorSubject.create<Boolean>()

    val observable: Observable<Boolean>
        get() = subject

    fun emit(favorite: Boolean) {
        subject.onNext(favorite)
    }
}
