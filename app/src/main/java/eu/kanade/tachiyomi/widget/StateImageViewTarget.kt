package eu.kanade.tachiyomi.widget

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil.target.ImageViewTarget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * A Coil target to display an image with an optional view to show while loading and a configurable
 * error drawable.
 *
 * @param view the view where the image will be loaded
 * @param progress an optional view to show when the image is loading.
 * @param errorDrawableRes the error drawable resource to show.
 * @param errorScaleType the scale type for the error drawable, [ScaleType.CENTER] by default.
 */
class StateImageViewTarget(
    view: ImageView,
    val progress: View? = null,
    private val errorDrawableRes: Int = R.drawable.ic_broken_image_grey_24dp,
    private val errorScaleType: ScaleType = ScaleType.CENTER
) : ImageViewTarget(view) {

    private val imageScaleType = view.scaleType

    override fun onStart(owner: LifecycleOwner) {
        progress?.isVisible = true
        super.onStart(owner)
    }

    override fun onError(error: Drawable?) {
        progress?.isVisible = false
        view.scaleType = errorScaleType

        val vector = AppCompatResources.getDrawable(view.context, errorDrawableRes)
        vector?.setTint(view.context.getResourceColor(R.attr.colorOnBackground, 0.38f))
        view.setImageDrawable(vector)
    }

    override fun onClear() {
        progress?.isVisible = false
        super.onClear()
    }

    override fun onSuccess(result: Drawable) {
        progress?.isVisible = false
        view.scaleType = imageScaleType
        super.onSuccess(result)
        view.setImageDrawable(result)
    }
}
