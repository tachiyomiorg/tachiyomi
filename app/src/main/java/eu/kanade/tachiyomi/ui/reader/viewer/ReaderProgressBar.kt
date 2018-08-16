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

class ReaderProgressBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Start at 10%
    private var sweepAngle = 10f

    private var aggregatedIsVisible = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getResourceColor(R.attr.colorAccent)
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private val ovalRect = RectF()

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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val diameter = Math.min(width, height)
        val thickness = diameter / 10f
        val pad = thickness / 2f
        ovalRect.set(pad, pad, diameter - pad, diameter - pad)

        paint.strokeWidth = thickness
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(ovalRect, -90f, sweepAngle, false, paint)
    }

    private fun calcSweepAngleFromProgress(progress: Int): Float {
        return 360f / 100 * progress
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)

        if (isVisible != aggregatedIsVisible) {
            aggregatedIsVisible = isVisible

            // let's be nice with the UI thread
            if (isVisible) {
                startAnimation()
            } else {
                stopAnimation()
            }
        }
    }

    private fun startAnimation() {
        if (visibility != View.VISIBLE || windowVisibility != View.VISIBLE || animation != null) {
            return
        }

        animation = rotationAnimation
        animation.start()
    }

    private fun stopAnimation() {
        clearAnimation()
    }

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

    fun completeAndFadeOut() {
        setRealProgress(100)
        hide(true)
    }

    /**
     * Set progress of the circular progress bar.
     * @param progress progress between 0 and 100.
     */
    fun setProgress(progress: Int) {
        // Scale progress in [10, 95] range
        val scaledProgress = 85 * progress / 100 + 10
        setRealProgress(scaledProgress)
    }

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
