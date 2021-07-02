package eu.kanade.tachiyomi.widget

import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.R
import com.google.android.material.appbar.AppBarLayout

class ElevationAppBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppBarLayout(context, attrs) {

    private var origStateAnimator: StateListAnimator? = null

    init {
        origStateAnimator = stateListAnimator
    }

    fun enableElevation(liftOnScroll: Boolean) {
        setElevation(liftOnScroll)
    }

    private fun setElevation(liftOnScroll: Boolean) {
        stateListAnimator = origStateAnimator
        isLiftOnScroll = liftOnScroll
    }

    fun disableElevation() {
        stateListAnimator = StateListAnimator().apply {
            val objAnimator = ObjectAnimator.ofFloat(this, "elevation", 0f)

            // Enabled and collapsible, but not collapsed means not elevated
            addState(
                intArrayOf(android.R.attr.enabled, R.attr.state_collapsible, -R.attr.state_collapsed),
                objAnimator
            )

            // Default enabled state
            addState(intArrayOf(android.R.attr.enabled), objAnimator)

            // Disabled state
            addState(IntArray(0), objAnimator)
        }
    }
}
