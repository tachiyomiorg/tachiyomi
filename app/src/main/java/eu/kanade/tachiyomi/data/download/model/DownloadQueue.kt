package eu.kanade.tachiyomi.data.download.model

import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadStore
import eu.kanade.tachiyomi.source.model.Page
import rx.Observable
import rx.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList

class DownloadQueue(
    private val store: DownloadStore,
    private val queue: MutableList<Download> = CopyOnWriteArrayList()
) : List<Download> by queue {

    private val statusSubject = PublishSubject.create<Download>()

    private val updatedRelay = PublishRelay.create<Unit>()

    fun addAll(downloads: List<Download>) {
        downloads.forEach { download ->
            download.setStatusSubject(statusSubject)
            download.setStatusCallback(::setPagesFor)
            download.status = Download.State.QUEUE
        }
        queue.addAll(downloads)
        store.addAll(downloads)
        updatedRelay.call(Unit)
    }

    fun remove(download: Download) {
        val removed = queue.remove(download)
        store.remove(download)
        download.setStatusSubject(null)
        download.setStatusCallback(null)
        if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
            download.status = Download.State.NOT_DOWNLOADED
        }
        if (removed) {
            updatedRelay.call(Unit)
        }
    }

    fun remove(chapter: Chapter) {
        find { it.chapter.id == chapter.id }?.let { remove(it) }
    }

    fun remove(chapters: List<Chapter>) {
        for (chapter in chapters) {
            remove(chapter)
        }
    }

    fun remove(manga: Manga) {
        filter { it.manga.id == manga.id }.forEach { remove(it) }
    }

    fun clear() {
        queue.forEach { download ->
            download.setStatusSubject(null)
            download.setStatusCallback(null)
            if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
                download.status = Download.State.NOT_DOWNLOADED
            }
        }
        queue.clear()
        store.clear()
        updatedRelay.call(Unit)
    }

    fun getActiveDownloads(): Observable<Download> =
        Observable.from(this).filter { download -> download.status == Download.State.DOWNLOADING }

    fun getStatusObservable(): Observable<Download> = statusSubject.onBackpressureBuffer()

    fun getUpdatedObservable(): Observable<List<Download>> = updatedRelay.onBackpressureBuffer()
        .startWith(Unit)
        .map { this }

    private fun setPagesFor(download: Download) {
        if (download.status == Download.State.DOWNLOADED || download.status == Download.State.ERROR) {
            setPagesSubject(download.pages, null)
        }
    }

    fun getProgressObservable(): Observable<Download> {
        return statusSubject.onBackpressureBuffer()
            .startWith(getActiveDownloads())
            .flatMap { download ->
                if (download.status == Download.State.DOWNLOADING) {
                    val pageStatusSubject = PublishSubject.create<Int>()
                    setPagesSubject(download.pages, pageStatusSubject)
                    return@flatMap pageStatusSubject
                        .onBackpressureBuffer()
                        .filter { it == Page.READY }
                        .map { download }
                } else if (download.status == Download.State.DOWNLOADED || download.status == Download.State.ERROR) {
                    setPagesSubject(download.pages, null)
                }
                Observable.just(download)
            }
            .filter { it.status == Download.State.DOWNLOADING }
    }

    private fun setPagesSubject(pages: List<Page>?, subject: PublishSubject<Int>?) {
        pages?.forEach { it.setStatusSubject(subject) }
    }
}
