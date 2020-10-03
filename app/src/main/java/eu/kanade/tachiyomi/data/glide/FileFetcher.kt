package eu.kanade.tachiyomi.data.glide

import android.content.ContentValues.TAG
import android.util.Log
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

open class FileFetcher(private val filePath: String = "") : DataFetcher<InputStream> {

    private var data: InputStream? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        loadFromFile(callback)
    }

    private fun loadFromFile(callback: DataFetcher.DataCallback<in InputStream>) {
        loadFromFile(File(filePath), callback)
    }

    protected fun loadFromFile(file: File, callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            data = FileInputStream(file)
        } catch (e: FileNotFoundException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Timber.d(e, "Failed to open file")
            }
            callback.onLoadFailed(e)
            return
        }

        callback.onDataReady(data)
    }

    override fun cleanup() {
        try {
            data?.close()
        } catch (e: IOException) {
            // Ignored.
        }
    }

    override fun cancel() {
        // Do nothing.
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}
