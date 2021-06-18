package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import rx.Completable
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber

/**
 * Loader used to retrieve the [PageLoader] for a given chapter.
 */
class ChapterLoader(
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val manga: Manga,
    private val source: Source
) {

    /**
     * Returns a completable that assigns the page loader and loads the its pages. It just
     * completes if the chapter is already loaded.
     */
    fun loadChapter(chapter: ReaderChapter): Completable {
        if (chapterIsReady(chapter)) {
            return Completable.complete()
        }

        return Observable.just(chapter)
            .doOnNext { chapter.state = ReaderChapter.State.Loading }
            .observeOn(Schedulers.io())
            .flatMap { readerChapter ->
                Timber.d("Loading pages for ${chapter.chapter.name}")

                val loader = getPageLoader(readerChapter)
                chapter.pageLoader = loader

                loader.getPages().take(1).doOnNext { pages ->
                    pages.forEach { it.chapter = chapter }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { chapter.state = ReaderChapter.State.Error(it) }
            .doOnNext { pages ->
                if (pages.isEmpty()) {
                    throw Exception(context.getString(R.string.page_list_empty_error))
                }

                chapter.state = ReaderChapter.State.Loaded(pages)

                // If the chapter is partially read, set the starting page to the last the user read
                // otherwise use the requested page.
                if (!chapter.chapter.read) {
                    chapter.requestedPage = chapter.chapter.last_page_read
                }
            }
            .toCompletable()
    }

    /**
     * Checks [chapter] to be loaded based on present pages and loader in addition to state.
     */
    private fun chapterIsReady(chapter: ReaderChapter): Boolean {
        return chapter.state is ReaderChapter.State.Loaded && chapter.pageLoader != null
    }

    /**
     * Returns the page loader to use for this [chapter].
     */
    private fun getPageLoader(chapter: ReaderChapter): PageLoader {
        val isDownloaded = downloadManager.isChapterDownloaded(chapter.chapter, manga, true)
        return when {
            isDownloaded -> DownloadPageLoader(chapter, manga, source, downloadManager)
            source is HttpSource -> HttpPageLoader(chapter, source)
            source is LocalSource -> source.getFormat(chapter.chapter).let { format ->
                when (format) {
                    is LocalSource.Format.Directory -> DirectoryPageLoader(format.file)
                    is LocalSource.Format.Zip -> ZipPageLoader(format.file)
                    is LocalSource.Format.Rar -> RarPageLoader(format.file)
                    is LocalSource.Format.Epub -> EpubPageLoader(format.file)
                }
            }
            else -> error(context.getString(R.string.loader_not_implemented_error))
        }
    }
}
