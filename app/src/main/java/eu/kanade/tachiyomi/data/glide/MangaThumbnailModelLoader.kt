package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.InputStream

/**
 * A class for loading a cover associated with a [Manga] that can be present in our own cache.
 * Coupled with [LibraryMangaUrlFetcher], this class allows to implement the following flow:
 *
 * - Check in RAM LRU.
 * - Check in disk LRU.
 * - Check in this module.
 * - Fetch from the network connection.
 *
 * @param context the application context.
 */
class MangaThumbnailModelLoader : ModelLoader<MangaThumbnail, InputStream> {

    /**
     * Cover cache where persistent covers are stored.
     */
    private val coverCache: CoverCache by injectLazy()

    /**
     * Source manager.
     */
    private val sourceManager: SourceManager by injectLazy()

    /**
     * Default network client.
     */
    private val defaultClient = Injekt.get<NetworkHelper>().client

    /**
     * Map where request headers are stored for a source.
     */
    private val cachedHeaders = hashMapOf<Long, LazyHeaders>()

    /**
     * Factory class for creating [MangaThumbnailModelLoader] instances.
     */
    class Factory : ModelLoaderFactory<MangaThumbnail, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<MangaThumbnail, InputStream> {
            return MangaThumbnailModelLoader()
        }

        override fun teardown() {}
    }

    override fun handles(model: MangaThumbnail): Boolean {
        return true
    }

    /**
     * Returns a fetcher for the given manga or null if the url is empty.
     *
     * @param mangaThumbnail the model.
     * @param width the width of the view where the resource will be loaded.
     * @param height the height of the view where the resource will be loaded.
     */
    override fun buildLoadData(
        mangaThumbnail: MangaThumbnail,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        val manga = mangaThumbnail.manga
        val url = manga.thumbnail_url

        if (url.isNullOrEmpty()) {
            return if (!manga.favorite || manga.isLocal()) {
                null
            } else {
                ModelLoader.LoadData(mangaThumbnail, LibraryMangaCustomCoverFetcher(manga, coverCache))
            }
        }

        if (url.startsWith("http", true)) {
            val source = sourceManager.get(manga.source) as? HttpSource
            val glideUrl = GlideUrl(url, getHeaders(manga, source))

            // Get the resource fetcher for this request url.
            val networkFetcher = OkHttpStreamFetcher(source?.client ?: defaultClient, glideUrl)

            if (!manga.favorite) {
                return ModelLoader.LoadData(glideUrl, networkFetcher)
            }

            val libraryFetcher = LibraryMangaUrlFetcher(networkFetcher, manga, coverCache)

            // Return an instance of the fetcher providing the needed elements.
            return ModelLoader.LoadData(mangaThumbnail, libraryFetcher)
        } else {
            // Return an instance of the fetcher providing the needed elements.
            return ModelLoader.LoadData(mangaThumbnail, FileFetcher(url.removePrefix("file://")))
        }
    }

    /**
     * Returns the request headers for a source copying its OkHttp headers and caching them.
     *
     * @param manga the model.
     */
    private fun getHeaders(manga: Manga, source: HttpSource?): Headers {
        if (source == null) return LazyHeaders.DEFAULT

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
