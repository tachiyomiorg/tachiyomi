package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.google.gson.Gson
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import uy.kohesive.injekt.injectLazy

/**
 * This class is used to persist active downloads across application restarts.
 *
 * @param context the application context.
 */
class DownloadStore(
        context: Context,
        private val sourceManager: SourceManager
) {

    /**
     * Preference file where active downloads are stored.
     */
    private val preferences = context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE)

    /**
     * Gson instance to serialize/deserialize downloads.
     */
    private val gson: Gson by injectLazy()

    /**
     * Database helper.
     */
    private val db: DatabaseHelper by injectLazy()

    /**
     * Counter used to keep the queue order.
     */
    private var counter = 0

    /**
     * Adds a list of downloads to the store.
     *
     * @param downloads the list of downloads to add.
     */
    fun addAll(downloads: List<Download>) {
        val editor = preferences.edit()
        downloads.forEach { editor.putString(getKey(it), serialize(it)) }
        editor.apply()
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
     * Removes all the downloads from the store.
     */
    fun clear() {
        preferences.edit().clear().apply()
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
                .mapNotNull { deserialize(it) }
                .sortedBy { it.order }

        val downloads = mutableListOf<Download>()
        if (objs.isNotEmpty()) {
            val cachedManga = mutableMapOf<Long, Manga?>()
            for ((mangaId, chapterId) in objs) {
                val manga = cachedManga.getOrPut(mangaId) {
                    db.getManga(mangaId).executeAsBlocking()
                } ?: continue
                val source = sourceManager.get(manga.source) as? HttpSource ?: continue
                val chapter = db.getChapter(chapterId).executeAsBlocking() ?: continue
                downloads.add(Download(source, manga, chapter))
            }
        }

        // Clear the store, downloads will be added again immediately.
        clear()
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
    private fun deserialize(string: String): DownloadObject? {
        return try {
            gson.fromJson(string, DownloadObject::class.java)
        } catch (e: Exception) {
            null
        }
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
