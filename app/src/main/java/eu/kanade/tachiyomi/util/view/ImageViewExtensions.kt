package eu.kanade.tachiyomi.util.view

import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * Set a vector on a [ImageView].
 *
 * @param drawable id of drawable resource
 */
fun ImageView.setVectorCompat(@DrawableRes drawable: Int, @AttrRes tint: Int? = null) {
    val vector = AppCompatResources.getDrawable(context, drawable)
    if (tint != null) {
        vector?.mutate()
        vector?.setTint(context.getResourceColor(tint))
    }
    setImageDrawable(vector)
}
