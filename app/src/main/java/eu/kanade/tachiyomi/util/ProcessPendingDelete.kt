package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.SourceManager
import rx.Observable
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

class ProcessPendingDelete {

    val db: DatabaseHelper by injectLazy()

    val downloadManager: DownloadManager by injectLazy()

    val sourceManager: SourceManager by injectLazy()

    private var isStarted = false

    fun processOnStartup() {
        if (!isStarted) {
            isStarted = true
            Observable.fromCallable {
                db.getPendingDeleteList().executeAsBlocking()
                        .map { db.getChapter(it.chapter_id).executeAsBlocking() }
                        .filter { it?.manga_id != null }
                        .forEach {
                            val chapter = it!!
                            val manga = db.getManga(chapter.manga_id!!).executeAsBlocking()
                            val source =
                                    if (manga != null) sourceManager.get(manga.source) else null
                            if (source != null)
                                downloadManager.deleteChapter(chapter, manga!!, source)
                        }
                db.deleteAllPendingDeleteItems().executeAsBlocking()
            }
                    .subscribeOn(Schedulers.io())
                    .subscribe()
        }
    }
}