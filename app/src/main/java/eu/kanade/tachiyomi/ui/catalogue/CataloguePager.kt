package eu.kanade.tachiyomi.ui.catalogue

import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.OnlineSource.Filter
import eu.kanade.tachiyomi.data.source.online.OnlineSource.FilterState
import rx.Observable

open class CataloguePager(val source: OnlineSource, val query: String, val filterStates: List<FilterState>): Pager() {

    override fun requestNext(transformer: (Observable<MangasPage>) -> Observable<MangasPage>): Observable<MangasPage> {
        val lastPage = lastPage

        val page = if (lastPage == null)
            MangasPage(1)
        else
            MangasPage(lastPage.page + 1).apply { url = lastPage.nextPageUrl!! }

        val observable = if (query.isBlank() && filterStates.all { it.state == it.filter.defaultState })
            source.fetchPopularManga(page)
        else
            source.fetchSearchManga(page, query, filterStates)

        return transformer(observable)
                .doOnNext { results.onNext(it) }
                .doOnNext { this@CataloguePager.lastPage = it }
    }

}