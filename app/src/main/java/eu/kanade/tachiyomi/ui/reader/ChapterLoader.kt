package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.fetchImageFromCacheThenNet
import eu.kanade.tachiyomi.source.online.fetchPageListFromCacheThenNet
import eu.kanade.tachiyomi.util.plusAssign
import rx.Observable
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class ChapterLoader(
        private val downloadManager: DownloadManager,
        private val manga: Manga,
        private val source: Source
) {

    private val queue = PriorityBlockingQueue<PriorityPage>()
    private val subscriptions = CompositeSubscription()

    fun init() {
        prepareOnlineReading()
    }

    fun restart() {
        cleanup()
        init()
    }

    fun cleanup() {
        subscriptions.clear()
        queue.clear()
    }

    private fun prepareOnlineReading() {
        if (source !is HttpSource) return

        subscriptions += Observable.defer { Observable.just(queue.take().page) }
                .filter { it.status == Page.QUEUE }
                .concatMap { source.fetchImageFromCacheThenNet(it) }
                .repeat()
                .subscribeOn(Schedulers.io())
                .subscribe({
                }, { error ->
                    if (error !is InterruptedException) {
                        Timber.e(error)
                    }
                })
    }

    fun loadChapter(chapter: ReaderChapter) = Observable.just(chapter)
            .flatMap {
                if (chapter.pages == null)
                    retrievePageList(chapter)
                else
                    Observable.just(chapter.pages!!)
            }
            .doOnNext { pages ->
                if (pages.isEmpty()) {
                    throw Exception("Page list is empty")
                }

                // Now that the number of pages is known, fix the requested page if the last one
                // was requested.
                if (chapter.requestedPage == -1) {
                    chapter.requestedPage = pages.lastIndex
                }

                loadPages(chapter)
            }
            .map { chapter }

    private fun retrievePageList(chapter: ReaderChapter) = Observable.just(chapter)
            .flatMap {
                // Check if the chapter is downloaded.
                chapter.isDownloaded = downloadManager.findChapterDir(source, manga, chapter) != null

                if (chapter.isDownloaded) {
                    // Fetch the page list from disk.
                    downloadManager.buildPageList(source, manga, chapter)
                } else {
                    (source as? HttpSource)?.fetchPageListFromCacheThenNet(chapter)
                            ?: source.fetchPageList(chapter)
                }
            }
            .doOnNext { pages ->
                chapter.pages = pages
                pages.forEach { it.chapter = chapter }
            }

    private fun loadPages(chapter: ReaderChapter) {
        if (!chapter.isDownloaded) {
            loadOnlinePages(chapter)
        }
    }

    private fun loadOnlinePages(chapter: ReaderChapter) {
        chapter.pages?.let { pages ->
            val startPage = chapter.requestedPage
            val pagesToLoad = if (startPage == 0)
                pages
            else
                pages.drop(startPage)

            pagesToLoad.forEach { queue.offer(PriorityPage(it, 0)) }
        }
    }

    fun loadPriorizedPage(page: Page) {
        queue.offer(PriorityPage(page, 1))
    }

    fun retryPage(page: Page) {
        queue.offer(PriorityPage(page, 2))
    }



    private data class PriorityPage(val page: Page, val priority: Int): Comparable<PriorityPage> {

        companion object {
            private val idGenerator = AtomicInteger()
        }

        private val identifier = idGenerator.incrementAndGet()

        override fun compareTo(other: PriorityPage): Int {
            val p = other.priority.compareTo(priority)
            return if (p != 0) p else identifier.compareTo(other.identifier)
        }

    }

}