package eu.kanade.tachiyomi.ui.reader.model

import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import timber.log.Timber

data class ReaderChapter(val chapter: Chapter) {

    var state: State =
            State.Wait
        set(value) {
            field = value
            stateRelay.call(value)
        }

    private val stateRelay by lazy { BehaviorRelay.create(state) }

    val stateObserver by lazy { stateRelay.asObservable() }

    val pages: List<ReaderPage>?
        get() = (state as? State.Loaded)?.pages

    var pageLoader: PageLoader? = null

    var requestedPage: Int = 0

    var references = 0
        private set

    fun ref() {
        references++
    }

    fun unref() {
        references--
        if (references == 0) {
            if (pageLoader != null) {
                Timber.d("Recycling chapter ${chapter.name}")
            }
            pageLoader?.recycle()
            pageLoader = null
            state = State.Wait
        }
    }

    sealed class State {
        object Wait : State()
        object Loading : State()
        class Error(val error: Throwable) : State()
        class Loaded(val pages: List<ReaderPage>) : State()
    }
}
