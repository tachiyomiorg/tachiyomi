package eu.kanade.tachiyomi.ui.main

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Keep

class ViewHeightAnimator(val view: View, val duration: Long = 250L) {

    /**
     * The default height of the view. It's unknown until the view is layout.
     */
    private var height = 0

    /**
     * Whether the last state of the view is shown or hidden.
     */
    private var isLastStateShown = true

    /**
     * Animation used to expand and collapse the view.
     */
    private val animation by lazy {
        ObjectAnimator.ofInt(this, "height", height).apply {
            duration = this@ViewHeightAnimator.duration
            interpolator = DecelerateInterpolator()
        }
    }

    init {
        view.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (view.height > 0) {
                        view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        // Save the tabs default height.
                        height = view.height

                        // Now that we know the height, set the initial height.
                        if (isLastStateShown) {
                            setHeight(height)
                        } else {
                            setHeight(0)
                        }
                    }
                }
            }
        )
    }

    /**
     * Sets the height of the tab layout.
     *
     * @param newHeight The new height of the tab layout.
     */
    @Keep
    fun setHeight(newHeight: Int) {
        view.layoutParams.height = newHeight
        view.requestLayout()
    }

    /**
     * Returns the height of the tab layout. This method is also called from the animator through
     * reflection.
     */
    fun getHeight(): Int {
        return view.layoutParams.height
    }

    /**
     * Expands the tab layout with an animation.
     */
    fun expand() {
        if (isMeasured) {
            if (getHeight() != height) {
                animation.setIntValues(height)
                animation.start()
            } else {
                animation.cancel()
            }
        }
        isLastStateShown = true
    }

    /**
     * Collapse the tab layout with an animation.
     */
    fun collapse() {
        if (isMeasured) {
            if (getHeight() != 0) {
                animation.setIntValues(0)
                animation.start()
            } else {
                animation.cancel()
            }
        }
        isLastStateShown = false
    }

    /**
     * Returns whether the tab layout has a known height.
     */
    private val isMeasured: Boolean
        get() = height > 0
}
