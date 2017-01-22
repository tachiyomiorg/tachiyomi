package eu.kanade.tachiyomi.ui.catalogue

import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import rx.Observable

/**
 * A general pager for source requests (latest updates, popular, search)
 */
abstract class Pager(var currentPage: Int = 1) {

    var hasNextPage = true
        private set

    protected val results: PublishRelay<Pair<Int, List<SManga>>> = PublishRelay.create()

    fun results(): Observable<Pair<Int, List<SManga>>> {
        return results.asObservable()
    }

    abstract fun requestNext(): Observable<MangasPage>

    fun onPageReceived(mangasPage: MangasPage) {
        val page = currentPage
        currentPage++
        hasNextPage = mangasPage.hasNextPage && !mangasPage.mangas.isEmpty()
        results.call(Pair(page, mangasPage.mangas))
    }

}