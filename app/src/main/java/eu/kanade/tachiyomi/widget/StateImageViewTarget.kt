package eu.kanade.tachiyomi.widget

import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import android.view.View
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.util.visible

/**
 * A glide target to display an image with an optional view to show while loading and a configurable
 * error drawable.
 *
 * @param view the view where the image will be loaded
 * @param progress an optional view to show when the image is loading.
 * @param errorDrawableRes the error drawable resource to show.
 * @param errorScaleType the scale type for the error drawable, [ScaleType.CENTER] by default.
 */
class StateImageViewTarget(view: ImageView,
                           val progress: View? = null,
                           val errorDrawableRes: Int = R.drawable.ic_broken_image_grey_24dp,
                           val errorScaleType: ScaleType = ScaleType.CENTER) :

        ImageViewTarget<Drawable>(view) {

    private var resource: Drawable? = null

    private val imageScaleType = view.scaleType

    override fun setResource(resource: Drawable?) {
        view.setImageDrawable(resource)
    }

    override fun onLoadStarted(placeholder: Drawable?) {
        progress?.visible()
        super.onLoadStarted(placeholder)
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        progress?.gone()
        view.scaleType = errorScaleType

        val vector = VectorDrawableCompat.create(view.context.resources, errorDrawableRes, null)
        vector?.setTint(view.context.getResourceColor(android.R.attr.textColorSecondary))
        view.setImageDrawable(vector)
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        progress?.gone()
        super.onLoadCleared(placeholder)
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        progress?.gone()
        view.scaleType = imageScaleType
        super.onResourceReady(resource, transition)
        this.resource = resource
    }
}
