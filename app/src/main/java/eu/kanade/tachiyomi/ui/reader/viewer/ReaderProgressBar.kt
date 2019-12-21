package eu.kanade.tachiyomi.ui.reader.viewer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.getResourceColor

/**
 * A custom progress bar that always rotates while being determinate. By always rotating we give
 * the feedback to the user that the application isn't 'stuck', and by making it determinate the
 * user also approximately knows how much the operation will take.
 */
class ReaderProgressBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * The current sweep angle. It always starts at 10% because otherwise the bar and the rotation
     * wouldn't be visible.
     */
    private var sweepAngle = 10f

    /**
     * Whether the parent views are also visible.
     */
    private var aggregatedIsVisible = false

    /**
     * The paint to use to draw the progress bar.
     */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getResourceColor(R.attr.colorAccent)
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    /**
     * The rectangle of the canvas where the progress bar should be drawn. This is calculated on
     * layout.
     */
    private val ovalRect = RectF()

    /**
     * The rotation animation to use while the progress bar is visible.
     */
    private val rotationAnimation by lazy {
        RotateAnimation(0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            duration = 4000
        }
    }

    /**
     * Called when the view is layout. The position and thickness of the progress bar is calculated.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val diameter = Math.min(width, height)
        val thickness = diameter / 10f
        val pad = thickness / 2f
        ovalRect.set(pad, pad, diameter - pad, diameter - pad)

        paint.strokeWidth = thickness
    }

    /**
     * Called when the view is being drawn. An arc is drawn with the calculated rectangle. The
     * animation will take care of rotation.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(ovalRect, -90f, sweepAngle, false, paint)
    }

    /**
     * Calculates the sweep angle to use from the progress.
     */
    private fun calcSweepAngleFromProgress(progress: Int): Float {
        return 360f / 100 * progress
    }

    /**
     * Called when this view is attached to window. It starts the rotation animation.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    /**
     * Called when this view is detached to window. It stops the rotation animation.
     */
    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    /**
     * Called when the visibility of this view changes.
     */
    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        val isVisible = visibility == View.VISIBLE
        if (isVisible) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    /**
     * Starts the rotation animation if needed.
     */
    private fun startAnimation() {
        if (visibility != View.VISIBLE || windowVisibility != View.VISIBLE || animation != null) {
            return
        }

        animation = rotationAnimation
        animation.start()
    }

    /**
     * Stops the rotation animation if needed.
     */
    private fun stopAnimation() {
        clearAnimation()
    }

    /**
     * Hides this progress bar with an optional fade out if [animate] is true.
     */
    fun hide(animate: Boolean = false) {
        if (visibility == View.GONE) return

        if (!animate) {
            visibility = View.GONE
        } else {
            ObjectAnimator.ofFloat(this, "alpha",  1f, 0f).apply {
                interpolator = DecelerateInterpolator()
                duration = 1000
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        visibility = View.GONE
                        alpha = 1f
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        alpha = 1f
                    }
                })
                start()
            }
        }
    }

    /**
     * Completes this progress bar and fades out the view.
     */
    fun completeAndFadeOut() {
        setRealProgress(100)
        hide(true)
    }

    /**
     * Set progress of the circular progress bar ensuring a min max range in order to notice the
     * rotation animation.
     */
    fun setProgress(progress: Int) {
        // Scale progress in [10, 95] range
        val scaledProgress = 85 * progress / 100 + 10
        setRealProgress(scaledProgress)
    }

    /**
     * Sets the real progress of the circular progress bar. Note that if this progres is 0 or
     * 100, the rotation animation won't be noticed by the user because nothing changes in the
     * canvas.
     */
    private fun setRealProgress(progress: Int) {
        ValueAnimator.ofFloat(sweepAngle, calcSweepAngleFromProgress(progress)).apply {
            interpolator = DecelerateInterpolator()
            duration = 250
            addUpdateListener { valueAnimator ->
                sweepAngle = valueAnimator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

}
