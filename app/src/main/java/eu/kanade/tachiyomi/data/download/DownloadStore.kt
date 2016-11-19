package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.google.gson.Gson
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import uy.kohesive.injekt.injectLazy

/**
 * This class is used to restore the active downloads after application restarts.
 *
 * @param context the application context.
 */
class DownloadStore(context: Context) {

    /**
     * Preference file where active downloads are stored.
     */
    private val preferences = context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE)

    /**
     * Gson instance to serialize/deserialize downloads.
     */
    private val gson: Gson by injectLazy()

    /**
     * Source manager.
     */
    private val sourceManager: SourceManager by injectLazy()

    /**
     * Database helper.
     */
    private val db: DatabaseHelper by injectLazy()

    /**
     * Counter used to keep the queue order.
     */
    private var counter = 0

    /**
     * Adds a download to the store.
     *
     * @param download the download to add.
     */
    fun add(download: Download) {
        preferences.edit()
                .putString(getKey(download), serialize(download))
                .apply()
    }

    /**
     * Removes a download from the store.
     *
     * @param download the download to remove.
     */
    fun remove(download: Download) {
        preferences.edit().remove(getKey(download)).apply()
    }

    /**
     * Returns the preference's key for the given download.
     *
     * @param download the download.
     */
    private fun getKey(download: Download): String {
        return download.chapter.id!!.toString()
    }

    /**
     * Returns the list of downloads to restore. It should be called in a background thread.
     */
    fun restore(): List<Download> {
        val objs = preferences.all
                .mapNotNull { it.value as? String }
                .map { deserialize(it) }
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

        // Clear the store, downloads will be added again immediately.
        preferences.edit().clear().apply()
        return downloads
    }

    /**
     * Converts a download to a string.
     *
     * @param download the download to serialize.
     */
    private fun serialize(download: Download): String {
        val obj = DownloadObject(download.manga.id!!, download.chapter.id!!, counter++)
        return gson.toJson(obj)
    }

    /**
     * Restore a download from a string.
     *
     * @param string the download as string.
     */
    private fun deserialize(string: String): DownloadObject {
        return gson.fromJson(string, DownloadObject::class.java)
    }

    /**
     * Class used for download serialization
     *
     * @param mangaId the id of the manga.
     * @param chapterId the id of the chapter.
     * @param order the order of the download in the queue.
     */
    data class DownloadObject(val mangaId: Long, val chapterId: Long, val order: Int)

}