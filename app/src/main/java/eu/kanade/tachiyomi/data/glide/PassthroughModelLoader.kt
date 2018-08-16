package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.IOException
import java.io.InputStream

class GlideInputStream(val fn: () -> InputStream)

class PassthroughModelLoader : ModelLoader<GlideInputStream, InputStream> {

    override fun buildLoadData(
            model: GlideInputStream,
            width: Int,
            height: Int,
            options: Options
    ): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(ObjectKey(model), Fetcher(model))
    }

    override fun handles(model: GlideInputStream): Boolean {
        return true
    }

    class Fetcher(private val streamFn: GlideInputStream) : DataFetcher<InputStream> {

        private var stream: InputStream? = null

        override fun getDataClass(): Class<InputStream> {
            return InputStream::class.java
        }

        override fun cleanup() {
            try {
                stream?.close()
            } catch (e: IOException) {
                // Do nothing
            }
        }

        override fun getDataSource(): DataSource {
            return DataSource.LOCAL
        }

        override fun cancel() {
            // Do nothing
        }

        override fun loadData(
                priority: Priority,
                callback: DataFetcher.DataCallback<in InputStream>
        ) {
            stream = streamFn.fn()
            callback.onDataReady(stream)
        }

    }

    /**
     * Factory class for creating [PassthroughModelLoader] instances.
     */
    class Factory : ModelLoaderFactory<GlideInputStream, InputStream> {

        override fun build(
                multiFactory: MultiModelLoaderFactory
        ): ModelLoader<GlideInputStream, InputStream> {
            return PassthroughModelLoader()
        }

        override fun teardown() {}
    }

}
