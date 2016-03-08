package eu.kanade.tachiyomi.util

import android.support.v4.content.ContextCompat
import android.widget.ImageView
import org.jetbrains.annotations.Nullable

/**
 * Set information image
 *
 * @param drawable id of image
 * @null makes image transparent
 */
fun ImageView.setInformationDrawable(@Nullable drawable: Int?) {
    if (drawable == null) {
        setImageResource(android.R.color.transparent)
    } else {
        setImageDrawable(ContextCompat.getDrawable(context, drawable))
    }
}