package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.model.Page
import rx.Observable
import rx.subjects.BehaviorSubject

class DownloadManager(context: Context) {


    private val provider = DownloadProvider(context)

    private val downloader = Downloader(context, provider)

    val queue: DownloadQueue
        get() = downloader.queue

    val isRunning: Boolean
        get() = downloader.isRunning

    val runningSubject: BehaviorSubject<Boolean>
        get() = downloader.runningSubject

    internal fun startDownloads(): Boolean {
        return downloader.start()
    }

    internal fun stopDownloads(errorMessage: String? = null) {
        downloader.stop(errorMessage)
    }

    fun clearQueue() {
        downloader.clearQueue()
    }

    fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        downloader.queueChapters(manga, chapters)
    }

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

    fun buildPageList(source: Source, manga: Manga, chapter: Chapter): Observable<List<Page>> {
        return buildPageList(provider.findChapterDir(source, manga, chapter))
    }

    fun getChapterDirName(chapter: Chapter): String {
        return provider.getChapterDirName(chapter)
    }

    fun findMangaDir(source: Source, manga: Manga): UniFile? {
        return provider.findMangaDir(source, manga)
    }

    fun findChapterDir(source: Source, manga: Manga, chapter: Chapter): UniFile? {
        return provider.findChapterDir(source, manga, chapter)
    }

    fun deleteChapter(source: Source, manga: Manga, chapter: Chapter) {
        provider.findChapterDir(source, manga, chapter)?.delete()
    }

}
