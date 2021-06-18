package eu.kanade.tachiyomi.util.system

import android.content.ClipData
import android.content.Intent
import android.net.Uri

fun Uri.toShareIntent(type: String = "image/*"): Intent {
    val uri = this
    return Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        setType(type)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
}
