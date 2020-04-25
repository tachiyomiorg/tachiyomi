package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.data.DataFetcher
import eu.kanade.tachiyomi.data.database.models.Manga
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * A [DataFetcher] for loading a cover of a library manga.
 * It tries to load the cover from our custom cache, and if it's not found, it fallbacks to network
 * and copies the result to the cache.
 *
 * @param networkFetcher the network fetcher for this cover.
 * @param manga the manga of the cover to load.
 * @param file the file where this cover should be. It may exists or not.
 */
class LibraryMangaUrlFetcher(
    private val networkFetcher: DataFetcher<InputStream>,
    private val manga: Manga,
    private val file: File
) :
    FileFetcher(file) {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        if (!file.exists()) {
            networkFetcher.loadData(
                priority,
                object : DataFetcher.DataCallback<InputStream> {
                    override fun onDataReady(data: InputStream?) {
                        if (data != null) {
                            val tmpFile = File(file.path + ".tmp")
                            try {
                                // Retrieve destination stream, create parent folders if needed.
                                val output = try {
                                    tmpFile.outputStream()
                                } catch (e: FileNotFoundException) {
                                    tmpFile.parentFile.mkdirs()
                                    tmpFile.outputStream()
                                }

                                // Copy the file and rename to the original.
                                data.use { output.use { data.copyTo(output) } }
                                tmpFile.renameTo(file)
                                loadFromFile(callback)
                            } catch (e: Exception) {
                                tmpFile.delete()
                                callback.onLoadFailed(e)
                            }
                        } else {
                            callback.onLoadFailed(Exception("Null data"))
                        }
                    }

                    override fun onLoadFailed(e: Exception) {
                        callback.onLoadFailed(e)
                    }
                }
            )
        } else {
            loadFromFile(callback)
        }
    }

    override fun cleanup() {
        super.cleanup()
        networkFetcher.cleanup()
    }

    override fun cancel() {
        super.cancel()
        networkFetcher.cancel()
    }
}
