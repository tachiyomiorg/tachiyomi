package eu.kanade.tachiyomi.ui.catalogue

import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.OnlineSource.Filter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

open class CataloguePager(val source: OnlineSource, val query: String, val filters: List<Filter<*>>) : Pager() {

    override fun requestNext(): Observable<MangasPage> {
        val lastPage = lastPage

        val page = if (lastPage == null)
            MangasPage(1)
        else
            MangasPage(lastPage.page + 1).apply { url = lastPage.nextPageUrl!! }

        val observable = if (query.isBlank() && filters.isEmpty())
            source.fetchPopularManga(page)
        else
            source.fetchSearchManga(page, query, filters)

        return observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { results.onNext(it) }
                .doOnNext { this@CataloguePager.lastPage = it }
    }

}