package eu.kanade.tachiyomi.ui.main

import android.support.design.widget.TabLayout
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.util.visible

class TabsAnimator(val tabs: TabLayout) {

    private var height = 0

    private val interpolator = DecelerateInterpolator()

    private val duration = 300L

    private val expandAnimation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            tabs.layoutParams.height = (height * interpolatedTime).toInt()
            tabs.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    private val collapseAnimation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            if (interpolatedTime == 1f) {
                tabs.gone()
            } else {
                tabs.layoutParams.height = (height * (1 - interpolatedTime)).toInt()
                tabs.requestLayout()
            }
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    init {
        collapseAnimation.duration = duration
        collapseAnimation.interpolator = interpolator
        expandAnimation.duration = duration
        expandAnimation.interpolator = interpolator
    }

    fun expand() {
        tabs.visible()
        if (measure() && tabs.measuredHeight != height) {
            tabs.startAnimation(expandAnimation)
        }
    }

    fun collapse() {
        if (measure() && tabs.measuredHeight != 0) {
            tabs.startAnimation(collapseAnimation)
        } else {
            tabs.gone()
        }
    }

    /**
     * Returns true if the view is measured, otherwise query dimensions and check again.
     */
    private fun measure(): Boolean {
        if (height > 0) return true
        height = tabs.measuredHeight
        return height > 0
    }
}