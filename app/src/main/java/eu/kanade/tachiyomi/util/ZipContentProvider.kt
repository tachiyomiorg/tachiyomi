package eu.kanade.tachiyomi.util

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import eu.kanade.tachiyomi.BuildConfig
import timber.log.Timber
import java.io.IOException
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.Executors

class ZipContentProvider : ContentProvider() {

    private val pool by lazy { Executors.newCachedThreadPool() }

    companion object {
        const val PROVIDER = "${BuildConfig.APPLICATION_ID}.zip-provider"
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun getType(uri: Uri): String? {
        return URLConnection.guessContentTypeFromName(uri.toString())
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        try {
            val url = "jar:file://" + uri.toString().substringAfter("content://$PROVIDER")
            val input = URL(url).openStream()
            val pipe = ParcelFileDescriptor.createPipe()
            pool.execute {
                try {
                    val output = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
                    input.use {
                        output.use {
                            input.copyTo(output)
                            output.flush()
                        }
                    }
                } catch (e: IOException) {
                    Timber.e(e)
                }
            }
            return AssetFileDescriptor(pipe[0], 0, -1)
        } catch (e: IOException) {
            return null
        }
    }

    override fun query(p0: Uri?, p1: Array<out String>?, p2: String?, p3: Array<out String>?, p4: String?): Cursor? {
        return null
    }

    override fun insert(p0: Uri?, p1: ContentValues?): Uri {
        throw UnsupportedOperationException("not implemented")
    }

    override fun update(p0: Uri?, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        throw UnsupportedOperationException("not implemented")
    }

    override fun delete(p0: Uri?, p1: String?, p2: Array<out String>?): Int {
        throw UnsupportedOperationException("not implemented")
    }
}