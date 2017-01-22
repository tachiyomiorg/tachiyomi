package eu.kanade.tachiyomi.data.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.GlideModule
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.network.NetworkHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream

/**
 * Class used to update Glide module settings
 */
class AppGlideModule : GlideModule {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Set the cache size of Glide to 15 MiB
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 15 * 1024 * 1024))
    }

    override fun registerComponents(context: Context, glide: Glide) {
        val networkFactory = OkHttpUrlLoader.Factory(Injekt.get<NetworkHelper>().client)

        glide.register(GlideUrl::class.java, InputStream::class.java, networkFactory)
        glide.register(Manga::class.java, InputStream::class.java, MangaModelLoader.Factory())
    }
}
