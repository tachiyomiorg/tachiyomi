package eu.kanade.tachiyomi.ui.latest_updates

import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.ui.catalogue.Pager
import rx.Observable

class LatestUpdatesPager(val source: OnlineSource): Pager() {

    override fun requestNext(transformer: (Observable<MangasPage>) -> Observable<MangasPage>): Observable<MangasPage> {
        val lastPage = lastPage

        val page = if (lastPage == null)
            MangasPage(1)
        else
            MangasPage(lastPage.page + 1).apply { url = lastPage.nextPageUrl!! }

        /* //in progress
        val observable = if (query.isBlank() && filters.isEmpty())
            source.fetchLatestUpdates(page)
        else
            source.fetchSearchManga(page, query, filters)
        */


        return transformer(source.fetchLatestUpdates(page))
                .doOnNext { results.onNext(it) }
                .doOnNext { this@LatestUpdatesPager.lastPage = it }
    }

}
