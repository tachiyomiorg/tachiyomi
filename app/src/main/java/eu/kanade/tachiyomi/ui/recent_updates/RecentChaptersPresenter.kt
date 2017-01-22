package eu.kanade.tachiyomi.ui.recent_updates

import android.os.Bundle
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.util.*

class RecentChaptersPresenter : BasePresenter<RecentChaptersFragment>() {
    /**
     * Used to connect to database
     */
    val db: DatabaseHelper by injectLazy()

    /**
     * Used to get settings
     */
    val preferences: PreferencesHelper by injectLazy()

    /**
     * Used to get information from download manager
     */
    val downloadManager: DownloadManager by injectLazy()

    /**
     * Used to get source from source id
     */
    val sourceManager: SourceManager by injectLazy()

    /**
     * List containing chapter and manga information
     */
    private var chapters: List<RecentChapter>? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        getRecentChaptersObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(RecentChaptersFragment::onNextRecentChapters)

        getChapterStatusObservable()
                .subscribeLatestCache(RecentChaptersFragment::onChapterStatusChange,
                        { view, error -> Timber.e(error) })

    }

    /**
     * Get observable containing recent chapters and date
     * @return observable containing recent chapters and date
     */
    fun getRecentChaptersObservable(): Observable<ArrayList<Any>> {
        // Set date for recent chapters
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.MONTH, -1)
        }

        return db.getRecentChapters(cal.time).asRxObservable()
                // Convert to a list of recent chapters.
                .map { mangaChapters ->
                    mangaChapters.map { it.toModel() }
                }
                .doOnNext {
                    setDownloadedChapters(it)
                    chapters = it
                }
                // Group chapters by the date they were fetched on a ordered map.
                .flatMap { recentItems ->
                    Observable.from(recentItems)
                            .toMultimap(
                                    { getMapKey(it.date_fetch) },
                                    { it },
                                    { TreeMap { d1, d2 -> d2.compareTo(d1) } })
                }
                // Add every day and all its chapters to a single list.
                .map { recentItems ->
                    ArrayList<Any>().apply {
                        for ((key, value) in recentItems) {
                            add(key)
                            addAll(value)
                        }
                    }
                }
    }

    /**
     * Returns observable containing chapter status.
     * @return download object containing download progress.
     */
    private fun getChapterStatusObservable(): Observable<Download> {
        return downloadManager.queue.getStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { download -> onDownloadStatusChange(download) }
    }

    /**
     * Converts a chapter from the database to an extended model, allowing to store new fields.
     */
    private fun MangaChapter.toModel(): RecentChapter {
        // Create the model object.
        val model = RecentChapter(this)

        // Find an active download for this chapter.
        val download = downloadManager.queue.find { it.chapter.id == chapter.id }

        // If there's an active download, assign it, otherwise ask the manager if the chapter is
        // downloaded and assign it to the status.
        if (download != null) {
            model.download = download
        }
        return model
    }

    /**
     * Finds and assigns the list of downloaded chapters.
     *
     * @param chapters the list of chapter from the database.
     */
    private fun setDownloadedChapters(chapters: List<RecentChapter>) {
        // Cached list of downloaded manga directories.
        val mangaDirectories = mutableMapOf<Long, Array<UniFile>>()

        // Cached list of downloaded chapter directories for a manga.
        val chapterDirectories = mutableMapOf<Long, Array<UniFile>>()

        for (chapter in chapters) {
            val manga = chapter.manga
            val source = sourceManager.get(manga.source) ?: continue

            val mangaDirs = mangaDirectories.getOrPut(source.id) {
                downloadManager.findSourceDir(source)?.listFiles() ?: emptyArray()
            }

            val mangaDirName = downloadManager.getMangaDirName(manga)
            val mangaDir = mangaDirs.find { it.name == mangaDirName } ?: continue

            val chapterDirs = chapterDirectories.getOrPut(manga.id!!) {
                mangaDir.listFiles() ?: emptyArray()
            }

            val chapterDirName = downloadManager.getChapterDirName(chapter)
            if (chapterDirs.any { it.name == chapterDirName }) {
                chapter.status = Download.DOWNLOADED
            }
        }
    }

    /**
     * Update status of chapters.
     * @param download download object containing progress.
     */
    private fun onDownloadStatusChange(download: Download) {
        // Assign the download to the model object.
        if (download.status == Download.QUEUE) {
            val chapter = chapters?.find { it.id == download.chapter.id }
            if (chapter != null && chapter.download == null) {
                chapter.download = download
            }
        }
    }

    /**
     * Get date as time key
     * @param date desired date
     * @return date as time key
     */
    private fun getMapKey(date: Long): Date {
        val cal = Calendar.getInstance()
        cal.time = Date(date)
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal.time
    }

    /**
     * Mark selected chapter as read
     * @param chapters list of selected chapters
     * @param read read status
     */
    fun markChapterRead(chapters: List<RecentChapter>, read: Boolean) {
        Observable.from(chapters)
                .doOnNext { chapter ->
                    chapter.read = read
                    if (!read) {
                        chapter.last_page_read = 0
                    }
                }
                .toList()
                .flatMap { db.updateChaptersProgress(it).asRxObservable() }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    /**
     * Delete selected chapters
     * @param chapters list of chapters
     */
    fun deleteChapters(chapters: List<RecentChapter>) {
        Observable.from(chapters)
                .doOnNext { deleteChapter(it) }
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst({ view, result ->
                    view.onChaptersDeleted()
                }, { view, error ->
                    view.onChaptersDeletedError(error)
                })
    }

    /**
     * Download selected chapters
     * @param chapters list of recent chapters seleted.
     */
    fun downloadChapters(chapters: List<RecentChapter>) {
        DownloadService.start(context)
        Observable.from(chapters)
                .doOnNext { downloadChapter(it) }
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

    /**
     * Download selected chapter
     * @param chapter chapter that is selected
     */
    fun downloadChapter(chapter: RecentChapter) {
        DownloadService.start(context)
        downloadManager.downloadChapters(chapter.manga, listOf(chapter))
    }

    /**
     * Delete selected chapter
     * @param chapter chapter that is selected
     */
    private fun deleteChapter(chapter: RecentChapter) {
        val source = sourceManager.get(chapter.manga.source) ?: return
        downloadManager.queue.remove(chapter)
        downloadManager.deleteChapter(source, chapter.manga, chapter)
        chapter.status = Download.NOT_DOWNLOADED
        chapter.download = null
    }

}