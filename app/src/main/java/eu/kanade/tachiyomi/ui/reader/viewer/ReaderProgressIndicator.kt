package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import androidx.annotation.IntRange
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.CircularProgressIndicator

/**
 * A wrapper for [CircularProgressIndicator] that always rotates while being determinate.
 *
 * By always rotating we give the feedback to the user that the application isn't 'stuck',
 * and by making it determinate the user also approximately knows how much the operation will take.
 */
class ReaderProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val indicator: CircularProgressIndicator

    private val rotateAnimation by lazy {
        RotateAnimation(
            0F,
            360F,
            Animation.RELATIVE_TO_SELF,
            0.5F,
            Animation.RELATIVE_TO_SELF,
            0.5F
        ).apply {
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            duration = 4000
        }
    }

    init {
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        indicator = CircularProgressIndicator(context)
        indicator.max = 100
        addView(indicator)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (indicator.isVisible && animation == null) {
            startAnimation(rotateAnimation)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearAnimation()
    }

    fun show() {
        indicator.show()
        if (animation == null) {
            startAnimation(rotateAnimation)
        }
    }

    fun hide() {
        indicator.hide()
        clearAnimation()
    }

    fun setProgress(@IntRange(from = 0, to = 100) progress: Int, animated: Boolean = true) {
        indicator.setProgressCompat(progress, animated)
    }
}
