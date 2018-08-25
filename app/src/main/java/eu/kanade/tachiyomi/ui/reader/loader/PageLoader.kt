package eu.kanade.tachiyomi.ui.reader.loader

import android.support.annotation.CallSuper
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import rx.Observable

/**
 * A loader used to load pages into the reader. Any open resources must be cleaned up when the
 * method [recycle] is called.
 */
abstract class PageLoader {

    /**
     * Whether this loader has been already recycled.
     */
    var isRecycled = false
        private set

    /**
     * Recycles this loader. Implementations must override this method to clean up any active
     * resources.
     */
    @CallSuper
    open fun recycle() {
        isRecycled = true
    }

    /**
     * Returns an observable containing the list of pages of a chapter. Only the first emission
     * will be used.
     */
    abstract fun getPages(): Observable<List<ReaderPage>>

    /**
     * Returns an observable that should inform of the progress of the page (see the Page class
     * for the available states)
     */
    abstract fun getPage(page: ReaderPage): Observable<Int>

    /**
     * Retries the given [page] in case it failed to load. This method only makes sense when an
     * online source is used.
     */
    open fun retryPage(page: ReaderPage) {}

}
