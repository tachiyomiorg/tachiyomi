package eu.kanade.tachiyomi.util

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import eu.kanade.tachiyomi.BuildConfig
import java.io.IOException
import java.net.URL
import kotlin.concurrent.thread

class ZipContentProvider : ContentProvider() {

    companion object {
        const val PROVIDER = "${BuildConfig.APPLICATION_ID}.zip-provider"
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun getType(uri: Uri): String? {
        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        try {
            val url = "jar:file://" + uri.toString().substringAfter("content://$PROVIDER")
            val input = URL(url).openStream()
            val pipe = ParcelFileDescriptor.createPipe()
            thread {
                val output = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
                input.use {
                    output.use {
                        try {
                            input.copyTo(output)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        output.flush()
                    }
                }
            }
            return AssetFileDescriptor(pipe[0], 0, -1)
        } catch (e: IOException) {
            return null
        }
    }

    override fun insert(p0: Uri?, p1: ContentValues?): Uri {
        throw UnsupportedOperationException("not implemented")
    }

    override fun query(p0: Uri?, p1: Array<out String>?, p2: String?, p3: Array<out String>?, p4: String?): Cursor {
        throw UnsupportedOperationException("not implemented")
    }

    override fun update(p0: Uri?, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        throw UnsupportedOperationException("not implemented")
    }

    override fun delete(p0: Uri?, p1: String?, p2: Array<out String>?): Int {
        throw UnsupportedOperationException("not implemented")
    }
}