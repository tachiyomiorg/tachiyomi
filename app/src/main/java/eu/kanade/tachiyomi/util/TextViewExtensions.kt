package eu.kanade.tachiyomi.util

import android.os.Build
import android.support.annotation.StyleRes
import android.widget.TextView

/**
 * Compat version vor setTextAppearance which is deprecated in API 23
 * @param resource Id of drawable resource
 */
fun TextView.setTextAppearanceCompat(@StyleRes resource: Int) {
    if (Build.VERSION.SDK_INT < 23) {
        @Suppress("DEPRECATION")
        this.setTextAppearance(context, resource)
    } else {
        this.setTextAppearance(resource)
    }
}