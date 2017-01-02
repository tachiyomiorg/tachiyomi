package eu.kanade.tachiyomi.ui.latest_updates

import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.ui.catalogue.Pager
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class LatestUpdatesPager(val source: OnlineSource): Pager() {

    override fun requestNext(): Observable<MangasPage> {
        val lastPage = lastPage

        val page = if (lastPage == null)
            MangasPage(1)
        else
            MangasPage(lastPage.page + 1).apply { url = lastPage.nextPageUrl!! }

        val observable = source.fetchLatestUpdates(page)

        return observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { results.onNext(it) }
                .doOnNext { this@LatestUpdatesPager.lastPage = it }
    }

}
