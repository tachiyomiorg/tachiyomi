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
        var drawable = ContextCompat.getDrawable(context, drawable)
        drawable.setTint(this.context.theme.getResourceColor(android.R.attr.textColorHint))
        setImageDrawable(drawable)
    } else {
        setImageResource(android.R.color.transparent)
    }
}
