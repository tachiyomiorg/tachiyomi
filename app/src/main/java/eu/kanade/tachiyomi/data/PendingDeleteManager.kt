package eu.kanade.tachiyomi.data

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.SourceManager
import rx.Observable
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

/**
 * This class is used to manage and schedule chapters deletion at app start
 * (if they can't be deleted instantly)
 * Part of Remove after read feature
 *
 * @param context the application context.
 */
class PendingDeleteManager(context: Context) {

    val db: DatabaseHelper by injectLazy()

    val downloadManager: DownloadManager by injectLazy()

    val sourceManager: SourceManager by injectLazy()

    private val valuePlaceholder = ""

    private val prefs =
            context.getSharedPreferences("chapters_pending_delete", Context.MODE_PRIVATE)

    fun addChapter(chapterId: Long?) {
        if (chapterId != null) prefs.edit()
                .putString(chapterId.toString(), valuePlaceholder)
                .apply()
    }

    fun removeChapter(chapterId: Long?) {
        if (chapterId != null) prefs.edit()
                .remove(chapterId.toString())
                .apply()
    }

    fun clearList() {
        prefs.edit().clear().apply()
    }

    private fun getMangaFromChapter(ch: Chapter): Observable<Pair<Chapter, Manga>> {
        return db.getManga(ch.manga_id!!).asRxObservable().map { Pair(ch, it) }
    }

    private fun getChapter(id: Long): Observable<Chapter> {
        return db.getChapter(id).asRxObservable()
    }

    fun deleteListed() {
        val list = prefs.all.keys
        clearList()

        Observable.from(list)
                .flatMap { getChapter(it.toLong()) }
                .flatMap { getMangaFromChapter(it) }
                .map { (chapter, manga) ->
                    val src = sourceManager.get(manga.source)
                    downloadManager.deleteChapter(chapter, manga, src!!)
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

}