package eu.kanade.tachiyomi.util.system

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.truncateCenter

/**
 * Copies a string to clipboard
 *
 * @param label Label to show to the user describing the content
 * @param content the actual text to copy to the board
 */
fun Activity.copyToClipboard(label: String, content: String) {
    if (content.isBlank()) return

    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, content))

    toast(getString(R.string.copied_to_clipboard, content.truncateCenter(50)))
}

/**
 * Checks whether if the device has a display cutout (i.e. notch, camera cutout, etc.).
 *
 * Only works in Android 9+.
 */
fun Activity.hasDisplayCutout(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        window.decorView.rootWindowInsets?.displayCutout != null
}
