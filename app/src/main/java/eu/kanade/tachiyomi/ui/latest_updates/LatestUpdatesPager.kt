package eu.kanade.tachiyomi.ui.latest_updates

import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.OnlineSource.Filter
import rx.Observable
import rx.subjects.PublishSubject

class LatestUpdatesPager(val source: OnlineSource, val query: String, val filters: List<Filter>) {

    private var lastPage: MangasPage? = null

    private val results = PublishSubject.create<MangasPage>()

    fun results(): Observable<MangasPage> {
        return results.asObservable()
    }

    fun requestNext(transformer: (Observable<MangasPage>) -> Observable<MangasPage>): Observable<MangasPage> {
        val lastPage = lastPage

        val page = if (lastPage == null)
            MangasPage(1)
        else
            MangasPage(lastPage.page + 1).apply { url = lastPage.nextPageUrl!! }

        val observable = if (query.isBlank() && filters.isEmpty())
            source.fetchLatestUpdates(page)
        else
            source.fetchSearchManga(page, query, filters)

        return transformer(observable)
                .doOnNext { results.onNext(it) }
                .doOnNext { this@LatestUpdatesPager.lastPage = it }
    }

    fun hasNextPage(): Boolean {
        return lastPage == null || lastPage?.nextPageUrl != null
    }

}