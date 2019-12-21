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

class PassthroughModelLoader : ModelLoader<InputStream, InputStream> {

    override fun buildLoadData(
            model: InputStream,
            width: Int,
            height: Int,
            options: Options
    ): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(ObjectKey(model), Fetcher(model))
    }

    override fun handles(model: InputStream): Boolean {
        return true
    }

    class Fetcher(private val stream: InputStream) : DataFetcher<InputStream> {

        override fun getDataClass(): Class<InputStream> {
            return InputStream::class.java
        }

        override fun cleanup() {
            try {
                stream.close()
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
            callback.onDataReady(stream)
        }

    }

    /**
     * Factory class for creating [PassthroughModelLoader] instances.
     */
    class Factory : ModelLoaderFactory<InputStream, InputStream> {

        override fun build(
                multiFactory: MultiModelLoaderFactory
        ): ModelLoader<InputStream, InputStream> {
            return PassthroughModelLoader()
        }

        override fun teardown() {}
    }

}
