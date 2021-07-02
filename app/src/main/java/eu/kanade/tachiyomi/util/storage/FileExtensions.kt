package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import java.io.File

fun getTempShareDir(context: Context) = File(context.cacheDir, "shared_image")

fun getPicturesDir(context: Context) = File(
    Environment.getExternalStorageDirectory().absolutePath +
        File.separator + Environment.DIRECTORY_PICTURES +
        File.separator + context.getString(R.string.app_name)
)

/**
 * Returns the uri of a file
 *
 * @param context context of application
 */
fun File.getUriCompat(context: Context): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", this)
    } else {
        this.toUri()
    }
}
