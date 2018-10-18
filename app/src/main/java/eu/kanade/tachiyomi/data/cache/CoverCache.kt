package eu.kanade.tachiyomi.data.cache

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.DiskUtil
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Class used to create cover cache.
 * It is used to store the covers of the library.
 * Makes use of Glide (which can avoid repeating requests) to download covers.
 * Names of files are created with the md5 of the thumbnail URL.
 *
 * @param context the application context.
 * @constructor creates an instance of the cover cache.
 */
class CoverCache(private val context: Context) {

    /**
     * Cache directory used for cache management.
     */
    private val cacheDir = context.getExternalFilesDir("covers") ?:
            File(context.filesDir, "covers").also { it.mkdirs() }

    /**
     * Returns the cover from cache.
     *
     * @param manga the manga to get cover of.
     * @return cover image.
     */
    fun getCoverFile(manga: Manga): File {
        val thumbnailUrl = manga.thumbnail_url ?: manga.url
        return File(cacheDir, DiskUtil.hashKeyForDisk(thumbnailUrl))
    }

    /**
     * Copy the given stream to this cache.
     *
     * @param manga the manga to change cover of.
     * @param inputStream  the stream to copy.
     * @throws IOException if there's any error.
     */
    @Throws(IOException::class)
    fun copyToCache(manga: Manga, inputStream: InputStream) {
        // Get destination file.
        val destFile = getCoverFile(manga)

        destFile.outputStream().use { inputStream.copyTo(it) }
    }

    /**
     * Delete the cover file from the cache.
     *
     * @param manga the manga to remove cover of.
     * @return status of deletion.
     */
    fun deleteFromCache(manga: Manga): Boolean {
        // Remove file.
        val file = getCoverFile(manga)
        return file.exists() && file.delete()
    }

}
