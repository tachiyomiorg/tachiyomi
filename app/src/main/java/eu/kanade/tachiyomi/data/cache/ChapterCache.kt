package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.DiskUtil
import eu.kanade.tachiyomi.util.saveTo
import okhttp3.Response
import okio.Okio
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException

/**
 * Class used to create chapter cache
 * For each image in a chapter a file is created
 * For each chapter a Json list is created and converted to a file.
 * The files are in format *md5key*.0
 *
 * @param context the application context.
 * @constructor creates an instance of the chapter cache.
 */
class ChapterCache(private val context: Context) {

    companion object {
        /** Name of cache directory.  */
        const val PARAMETER_CACHE_DIRECTORY = "chapter_disk_cache"

        /** Application cache version.  */
        const val PARAMETER_APP_VERSION = 1

        /** The number of values per cache entry. Must be positive.  */
        const val PARAMETER_VALUE_COUNT = 1

        /** The maximum number of bytes this cache should use to store.  */
        const val PARAMETER_CACHE_SIZE = 75L * 1024 * 1024
    }

    /** Google Json class used for parsing JSON files.  */
    private val gson: Gson by injectLazy()

    /** Cache class used for cache management.  */
    private val diskCache = DiskLruCache.open(
            File(context.externalCacheDir, PARAMETER_CACHE_DIRECTORY),
            PARAMETER_APP_VERSION,
            PARAMETER_VALUE_COUNT,
            PARAMETER_CACHE_SIZE)

    /**
     * Returns directory of cache.
     */
    val cacheDir: File
        get() = diskCache.directory

    /**
     * Returns real size of directory.
     */
    private val realSize: Long
        get() = DiskUtil.getDirectorySize(cacheDir)

    /**
     * Returns real size of directory in human readable format.
     */
    val readableSize: String
        get() = Formatter.formatFileSize(context, realSize)

    /**
     * Remove file from cache.
     *
     * @param file name of file "md5.0".
     * @return status of deletion for the file.
     */
    fun removeFileFromCache(file: String): Boolean {
        // Make sure we don't delete the journal file (keeps track of cache).
        if (file == "journal" || file.startsWith("journal."))
            return false

        try {
            // Remove the extension from the file to get the key of the cache
            val key = file.substring(0, file.lastIndexOf("."))
            // Remove file from cache.
            return diskCache.remove(key)
        } catch (e: IOException) {
            return false
        }
    }

    /**
     * Get page list from cache.
     *
     * @param chapter the chapter.
     * @return an observable of the list of pages.
     */
    fun getPageListFromCache(chapter: Chapter): Observable<List<Page>> {
        return Observable.fromCallable {
            // Get the key for the chapter.
            val key = DiskUtil.hashKeyForDisk(getKey(chapter))

            // Convert JSON string to list of objects. Throws an exception if snapshot is null
            diskCache.get(key).use {
                gson.fromJson<List<Page>>(it.getString(0))
            }
        }
    }

    /**
     * Add page list to disk cache.
     *
     * @param chapter the chapter.
     * @param pages list of pages.
     */
    fun putPageListToCache(chapter: Chapter, pages: List<Page>) {
        // Convert list of pages to json string.
        val cachedValue = gson.toJson(pages)

        // Initialize the editor (edits the values for an entry).
        var editor: DiskLruCache.Editor? = null

        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(getKey(chapter))
            editor = diskCache.edit(key) ?: return

            // Write chapter urls to cache.
            Okio.buffer(Okio.sink(editor.newOutputStream(0))).use {
                it.write(cachedValue.toByteArray())
                it.flush()
            }

            diskCache.flush()
            editor.commit()
            editor.abortUnlessCommitted()

        } catch (e: Exception) {
            // Ignore.
        } finally {
            editor?.abortUnlessCommitted()
        }
    }

    /**
     * Returns true if image is in cache.
     *
     * @param imageUrl url of image.
     * @return true if in cache otherwise false.
     */
    fun isImageInCache(imageUrl: String): Boolean {
        try {
            return diskCache.get(DiskUtil.hashKeyForDisk(imageUrl)) != null
        } catch (e: IOException) {
            return false
        }
    }

    /**
     * Get image file from url.
     *
     * @param imageUrl url of image.
     * @return path of image.
     */
    fun getImageFile(imageUrl: String): File {
        // Get file from md5 key.
        val imageName = DiskUtil.hashKeyForDisk(imageUrl) + ".0"
        return File(diskCache.directory, imageName)
    }

    /**
     * Add image to cache.
     * 
     * @param imageUrl url of image.
     * @param response http response from page.
     * @throws IOException image error.
     */
    @Throws(IOException::class)
    fun putImageToCache(imageUrl: String, response: Response) {
        // Initialize editor (edits the values for an entry).
        var editor: DiskLruCache.Editor? = null

        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(imageUrl)
            editor = diskCache.edit(key) ?: throw IOException("Unable to edit key")

            // Get OutputStream and write image with Okio.
            response.body().source().saveTo(editor.newOutputStream(0))

            diskCache.flush()
            editor.commit()
        } finally {
            response.body().close()
            editor?.abortUnlessCommitted()
        }
    }

    private fun getKey(chapter: Chapter): String {
        return "${chapter.manga_id}${chapter.url}"
    }
}

