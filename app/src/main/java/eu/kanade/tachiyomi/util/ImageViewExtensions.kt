package eu.kanade.tachiyomi.util

import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.widget.ImageView

/**
 * Set a drawable on a [ImageView] using [ContextCompat] for backwards compatibility.
 *
 * @param drawable id of drawable resource
 */
fun ImageView.setDrawableCompat(@DrawableRes drawable: Int?) {
    if (drawable != null) {
        setImageDrawable(ContextCompat.getDrawable(context, drawable))
    } else {
        setImageResource(android.R.color.transparent)
    }
}
