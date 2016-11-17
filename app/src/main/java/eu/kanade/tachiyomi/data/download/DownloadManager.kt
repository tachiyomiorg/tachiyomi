package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.model.Page
import rx.Observable

/**
 * This class is used to manage chapter downloads in the application. It must be instantiated once
 * and retrieved through dependency injection. You can use this class to queue new chapters or query
 * downloaded chapters.
 *
 * @param context the application context.
 */
class DownloadManager(context: Context) {

    /**
     * Downloads provider, used to retrieve the folders where the chapters are or should be stored.
     */
    private val provider = DownloadProvider(context)

    /**
     * Downloader whose only task is to download chapters.
     */
    private val downloader = Downloader(context, provider)

    /**
     * Downloads queue, where the pending chapters are stored.
     */
    val queue: DownloadQueue
        get() = downloader.queue

    /**
     * Subject for subscribing to downloader status.
     */
    val runningRelay: BehaviorRelay<Boolean>
        get() = downloader.runningRelay

    /**
     * Tells the downloader to begin downloads.
     *
     * @return true if it's started, false otherwise (empty queue).
     */
    fun startDownloads(): Boolean {
        return downloader.start()
    }

    /**
     * Tells the downloader to stop downloads.
     *
     * @param reason an optional reason for being stopped, used to notify the user.
     */
    fun stopDownloads(reason: String? = null) {
        downloader.stop(reason)
    }

    /**
     * Empties the download queue.
     */
    fun clearQueue() {
        downloader.clearQueue()
    }

    /**
     * Tells the downloader to enqueue the given list of chapters.
     *
     * @param manga the manga of the chapters.
     * @param chapters the list of chapters to enqueue.
     */
    fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        downloader.queueChapters(manga, chapters)
    }

    /**
     * Builds the page list of a downloaded chapter.
     *
     * @param source the source of the chapter.
     * @param manga the manga of the chapter.
     * @param chapter the downloaded chapter.
     * @return an observable containing the list of pages from the chapter.
     */
    fun buildPageList(source: Source, manga: Manga, chapter: Chapter): Observable<List<Page>> {
        return buildPageList(provider.findChapterDir(source, manga, chapter))
    }

    /**
     * Builds the page list of a downloaded chapter.
     *
     * @param chapterDir the file where the chapter is downloaded.
     * @return an observable containing the list of pages from the chapter.
     */
    private fun buildPageList(chapterDir: UniFile?): Observable<List<Page>> {
        return Observable.fromCallable {
            val pages = mutableListOf<Page>()
            chapterDir?.listFiles()
                    ?.filter { it.type?.startsWith("image") ?: false }
                    ?.forEach { file ->
                        val page = Page(pages.size, uri = file.uri)
                        pages.add(page)
                        page.status = Page.READY
                    }
            pages
        }
    }

    /**
     * Returns the directory name for the given chapter.
     *
     * @param chapter the chapter to query.
     */
    fun getChapterDirName(chapter: Chapter): String {
        return provider.getChapterDirName(chapter)
    }

    /**
     * Returns the directory for the given manga, if it exists.
     *
     * @param source the source of the manga.
     * @param manga the manga to query.
     */
    fun findMangaDir(source: Source, manga: Manga): UniFile? {
        return provider.findMangaDir(source, manga)
    }

    /**
     * Returns the directory for the given chapter, if it exists.
     *
     * @param source the source of the chapter.
     * @param manga the manga of the chapter.
     * @param chapter the chapter to query.
     */
    fun findChapterDir(source: Source, manga: Manga, chapter: Chapter): UniFile? {
        return provider.findChapterDir(source, manga, chapter)
    }

    /**
     * Deletes the directory of a downloaded chapter.
     *
     * @param source the source of the chapter.
     * @param manga the manga of the chapter.
     * @param chapter the chapter to delete.
     */
    fun deleteChapter(source: Source, manga: Manga, chapter: Chapter) {
        provider.findChapterDir(source, manga, chapter)?.delete()
    }

}
