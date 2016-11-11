package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.google.gson.Gson
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import uy.kohesive.injekt.injectLazy

class DownloadStore(context: Context) {

    private val preferences = context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE)

    private val gson: Gson by injectLazy()

    private val sourceManager: SourceManager by injectLazy()

    private val db: DatabaseHelper by injectLazy()

    private var counter = 0

    fun add(download: Download) {
        preferences.edit()
                .putString(getKey(download), encode(download))
                .apply()
    }

    fun restore(): List<Download> {
        val objs = preferences.all
                .mapNotNull { it.value as? String }
                .map { decode(it) }
                .sortedBy { it.order }

        val downloads = mutableListOf<Download>()
        if (objs.isNotEmpty()) {
            val cachedManga = mutableMapOf<Long, Manga?>()
            for ((mangaId, chapterId) in objs) {
                val manga = cachedManga.getOrPut(mangaId) {
                    db.getManga(mangaId).executeAsBlocking()
                } ?: continue
                val source = sourceManager.get(manga.source) as? OnlineSource ?: continue
                val chapter = db.getChapter(chapterId).executeAsBlocking() ?: continue
                downloads.add(Download(source, manga, chapter))
            }
        }
        preferences.edit().clear().apply()
        return downloads
    }

    fun remove(download: Download) {
        preferences.edit().remove(getKey(download)).apply()
    }

    private fun getKey(download: Download): String {
        return download.chapter.id!!.toString()
    }

    private fun encode(download: Download): String {
        val obj = DownloadObject(download.manga.id!!, download.chapter.id!!, counter++)
        return gson.toJson(obj)
    }

    private fun decode(string: String): DownloadObject {
        return gson.fromJson(string, DownloadObject::class.java)
    }

    data class DownloadObject(val mangaId: Long, val chapterId: Long, val order: Int)

}