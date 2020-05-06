package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.data.DataFetcher
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import java.io.File
import java.io.InputStream
import java.lang.Exception

open class LibraryMangaCustomCoverFetcher(
    private val manga: Manga,
    private val coverCache: CoverCache
) : FileFetcher() {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        getCustomCoverFile()?.let {
            loadFromFile(it, callback)
        } ?: callback.onLoadFailed(Exception("Custom cover file not found"))
    }

    protected fun getCustomCoverFile(): File? {
        return coverCache.getCustomCoverFile(manga).takeIf { it.exists() }
    }
}
