package eu.kanade.tachiyomi.data.download.model

import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.DownloadStore
import eu.kanade.tachiyomi.data.source.model.Page
import rx.Observable
import rx.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList

class DownloadQueue(
        private val store: DownloadStore,
        private val queue: MutableList<Download> = CopyOnWriteArrayList<Download>())
: List<Download> by queue {

    private val statusSubject = PublishSubject.create<Download>()

    private val updatedRelay = PublishRelay.create<Unit>()

    fun add(download: Download) {
        download.setStatusSubject(statusSubject)
        download.status = Download.QUEUE
        store.add(download)
        val added = queue.add(download)
        if (added) {
            updatedRelay.call(Unit)
        }
    }

    fun del(download: Download) {
        val removed = queue.remove(download)
        store.remove(download)
        download.setStatusSubject(null)
        if (removed) {
            updatedRelay.call(Unit)
        }
    }

    fun del(chapter: Chapter) {
        find { it.chapter.id == chapter.id }?.let { del(it) }
    }

    fun clear() {
        queue.forEach { del(it) }
    }

    fun getActiveDownloads(): Observable<Download> =
        Observable.from(this).filter { download -> download.status == Download.DOWNLOADING }

    fun getStatusObservable(): Observable<Download> = statusSubject.onBackpressureBuffer()

    fun getUpdatedObservable(): Observable<Unit> = updatedRelay.onBackpressureBuffer()

    fun getProgressObservable(): Observable<Download> {
        return statusSubject.onBackpressureBuffer()
                .startWith(getActiveDownloads())
                .flatMap { download ->
                    if (download.status == Download.DOWNLOADING) {
                        val pageStatusSubject = PublishSubject.create<Int>()
                        setPagesSubject(download.pages, pageStatusSubject)
                        return@flatMap pageStatusSubject
                                .filter { it == Page.READY }
                                .map { download }

                    } else if (download.status == Download.DOWNLOADED || download.status == Download.ERROR) {
                        setPagesSubject(download.pages, null)
                    }
                    Observable.just(download)
                }
                .filter { it.status == Download.DOWNLOADING }
    }

    private fun setPagesSubject(pages: List<Page>?, subject: PublishSubject<Int>?) {
        if (pages != null) {
            for (page in pages) {
                page.setStatusSubject(subject)
            }
        }
    }

}
