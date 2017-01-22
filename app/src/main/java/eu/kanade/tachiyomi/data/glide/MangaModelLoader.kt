package eu.kanade.tachiyomi.data.glide

import android.content.Context
import android.util.LruCache
import com.bumptech.glide.Glide
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.*
import com.bumptech.glide.load.model.stream.StreamModelLoader
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.InputStream

/**
 * A class for loading a cover associated with a [Manga] that can be present in our own cache.
 * Coupled with [MangaDataFetcher], this class allows to implement the following flow:
 *
 * - Check in RAM LRU.
 * - Check in disk LRU.
 * - Check in this module.
 * - Fetch from the network connection.
 *
 * @param context the application context.
 */
class MangaModelLoader(context: Context) : StreamModelLoader<Manga> {

    /**
     * Cover cache where persistent covers are stored.
     */
    val coverCache: CoverCache by injectLazy()

    /**
     * Source manager.
     */
    val sourceManager: SourceManager by injectLazy()

    /**
     * Base network loader.
     */
    private val baseLoader = Glide.buildModelLoader(GlideUrl::class.java,
            InputStream::class.java, context)

    /**
     * LRU cache whose key is the thumbnail url of the manga, and the value contains the request url
     * and the file where it should be stored in case the manga is a favorite.
     */
    private val lruCache = LruCache<String, Pair<GlideUrl, File>>(100)

    /**
     * Map where request headers are stored for a source.
     */
    private val cachedHeaders = hashMapOf<Long, LazyHeaders>()

    /**
     * Factory class for creating [MangaModelLoader] instances.
     */
    class Factory : ModelLoaderFactory<Manga, InputStream> {

        override fun build(context: Context, factories: GenericLoaderFactory)
                = MangaModelLoader(context)

        override fun teardown() {}
    }

    /**
     * Returns a [MangaDataFetcher] for the given manga or null if the url is empty.
     *
     * @param manga the model.
     * @param width the width of the view where the resource will be loaded.
     * @param height the height of the view where the resource will be loaded.
     */
    override fun getResourceFetcher(manga: Manga,
                                    width: Int,
                                    height: Int): DataFetcher<InputStream>? {

        // Check thumbnail is not null or empty
        val url = manga.thumbnail_url
        if (url.isNullOrEmpty()) {
            return null
        }

        // Obtain the request url and the file for this url from the LRU cache, or calculate it
        // and add them to the cache.
        val (glideUrl, file) = lruCache.get(url) ?:
            Pair(GlideUrl(url, getHeaders(manga)), coverCache.getCoverFile(url!!)).apply {
                lruCache.put(url, this)
            }

        // Get the network fetcher for this request url.
        val networkFetcher = baseLoader.getResourceFetcher(glideUrl, width, height)

        // Return an instance of our fetcher providing the needed elements.
        return MangaDataFetcher(networkFetcher, file, manga)
    }

    /**
     * Returns the request headers for a source copying its OkHttp headers and caching them.
     *
     * @param manga the model.
     */
    fun getHeaders(manga: Manga): Headers {
        val source = sourceManager.get(manga.source) as? HttpSource ?: return LazyHeaders.DEFAULT
        return cachedHeaders.getOrPut(manga.source) {
            LazyHeaders.Builder().apply {
                val nullStr: String? = null
                setHeader("User-Agent", nullStr)
                for ((key, value) in source.headers.toMultimap()) {
                    addHeader(key, value[0])
                }
            }.build()
        }
    }

}
