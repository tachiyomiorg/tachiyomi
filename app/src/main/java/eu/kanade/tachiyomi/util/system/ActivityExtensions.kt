package eu.kanade.tachiyomi.util.system

import android.app.Activity
import android.os.Build

/**
 * Checks whether if the device has a display cutout (i.e. notch, camera cutout, etc.).
 *
 * Only works in Android 9+.
 */
fun Activity.hasDisplayCutout(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        window.decorView.rootWindowInsets?.displayCutout != null
}
