package eu.kanade.tachiyomi.ui.main

import android.animation.ObjectAnimator
import com.google.android.material.tabs.TabLayout
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator

class TabsAnimator(val tabs: TabLayout) {

    /**
     * The default height of the tab layout. It's unknown until the view is layout.
     */
    private var tabsHeight = 0

    /**
     * Whether the last state of the tab layout is shown or hidden.
     */
    private var isLastStateShown = true

    /**
     * Animation used to expand and collapse the tab layout.
     */
    private val animation by lazy {
        ObjectAnimator.ofInt(this, "height", tabsHeight).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
        }
    }

    init {
        tabs.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (tabs.height > 0) {
                        tabs.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        // Save the tabs default height.
                        tabsHeight = tabs.height

                        // Now that we know the height, set the initial height.
                        if (isLastStateShown) {
                            setHeight(tabsHeight)
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
    fun setHeight(newHeight: Int) {
        tabs.layoutParams.height = newHeight
        tabs.requestLayout()
    }

    /**
     * Returns the height of the tab layout. This method is also called from the animator through
     * reflection.
     */
    fun getHeight(): Int {
        return tabs.layoutParams.height
    }

    /**
     * Expands the tab layout with an animation.
     */
    fun expand() {
        if (isMeasured) {
            if (getHeight() != tabsHeight) {
                animation.setIntValues(tabsHeight)
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
        get() = tabsHeight > 0

}
