package eu.kanade.tachiyomi.widget

import android.content.Context
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import eu.kanade.tachiyomi.R

@Suppress("unused", "UNUSED_PARAMETER")
class FABAnimationUpDown @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : FABAnimationBase() {

    private val INTERPOLATOR = FastOutSlowInInterpolator()

    private val outAnimation by lazy {
        AnimationUtils.loadAnimation(ctx, R.anim.fab_hide_to_bottom).apply {
            duration = 200
            interpolator = INTERPOLATOR
        }
    }
    private val inAnimation by lazy {
        AnimationUtils.loadAnimation(ctx, R.anim.fab_show_from_bottom).apply {
            duration = 200
            interpolator = INTERPOLATOR
        }
    }

    override fun animateOut(button: FloatingActionButton) {
        outAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                isAnimatingOut = true
            }

            override fun onAnimationEnd(animation: Animation) {
                isAnimatingOut = false
                button.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })
        button.startAnimation(outAnimation)
    }

    override fun animateIn(button: FloatingActionButton) {
        button.visibility = View.VISIBLE
        button.startAnimation(inAnimation)
    }
}
