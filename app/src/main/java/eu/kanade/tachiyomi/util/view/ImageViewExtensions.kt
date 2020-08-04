package eu.kanade.tachiyomi.util.view

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

/**
 * Set a vector on a [ImageView].
 *
 * @param drawable id of drawable resource
 */
fun ImageView.setVectorCompat(@DrawableRes drawable: Int, tint: Int? = null) {
    val vector = AppCompatResources.getDrawable(context, drawable)
    if (tint != null) {
        vector?.mutate()
        vector?.setTint(tint)
    }
    setImageDrawable(vector)
}
