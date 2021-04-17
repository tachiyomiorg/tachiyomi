package eu.kanade.tachiyomi.widget

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import coil.drawable.CrossfadeDrawable
import coil.target.ImageViewTarget

/**
 * A Coil target to display an image with an optional view to show while loading.
 *
 * @param target the view where the image will be loaded
 * @param progress the view to show when the image is loading.
 * @param crossfadeDuration duration in millisecond to crossfade the result drawable
 */
class StateImageViewTarget(
    private val target: ImageView,
    private val progress: View,
    private val crossfadeDuration: Int = 0
) : ImageViewTarget(target) {
    override fun onStart(placeholder: Drawable?) {
        progress.isVisible = true
    }

    override fun onSuccess(result: Drawable) {
        progress.isVisible = false
        if (crossfadeDuration > 0) {
            val crossfadeResult = CrossfadeDrawable(target.drawable, result, durationMillis = crossfadeDuration)
            target.setImageDrawable(crossfadeResult)
            crossfadeResult.start()
        } else {
            target.setImageDrawable(result)
        }
    }

    override fun onError(error: Drawable?) {
        progress.isVisible = false
        if (error != null) {
            target.setImageDrawable(error)
        }
    }
}
