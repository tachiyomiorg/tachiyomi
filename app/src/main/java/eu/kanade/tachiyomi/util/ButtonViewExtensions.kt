package eu.kanade.tachiyomi.util

import android.support.annotation.DrawableRes
import android.support.graphics.drawable.VectorDrawableCompat
import android.widget.Button

/**
 * Sets android:drawableTop xml attribute programmatically
 * @param drawable Id of drawable resource
 */
fun Button.setDrawableTop(@DrawableRes drawable: Int, tint: Int? = null) {
    val top = VectorDrawableCompat.create(resources, drawable, context.theme)
    if (tint != null) {
        top?.setTint(tint)
    }
    setCompoundDrawablesWithIntrinsicBounds(null, top, null, null)
}