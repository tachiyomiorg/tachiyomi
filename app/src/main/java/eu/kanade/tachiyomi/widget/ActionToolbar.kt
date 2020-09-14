package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.common_action_toolbar.view.common_action_menu
import kotlinx.android.synthetic.main.common_action_toolbar.view.common_action_toolbar

/**
 * A toolbar holding only menu items.
 */
class ActionToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    init {
        inflate(context, R.layout.common_action_toolbar, this)
    }

    /**
     * Remove menu items and remove listener.
     */
    fun destroy() {
        common_action_menu.menu.clear()
        common_action_menu.setOnMenuItemClickListener(null)
    }

    /**
     * Gets a menu item if found.
     */
    fun findItem(@IdRes itemId: Int): MenuItem? {
        return common_action_menu.menu.findItem(itemId)
    }

    /**
     * Show the menu toolbar using the provided ActionMode's context to inflate the items.
     */
    fun show(mode: ActionMode, @MenuRes menuRes: Int, listener: (item: MenuItem?) -> Boolean) {
        // Avoid re-inflating the menu
        if (common_action_menu.menu.size() == 0) {
            mode.menuInflater.inflate(menuRes, common_action_menu.menu)
            common_action_menu.setOnMenuItemClickListener { listener(it) }
        }

        common_action_toolbar.isVisible = true
        val bottomAnimation = AnimationUtils.loadAnimation(context, R.anim.enter_from_bottom)
        common_action_toolbar.startAnimation(bottomAnimation)
    }

    /**
     * Hide the menu toolbar.
     */
    fun hide() {
        val bottomAnimation = AnimationUtils.loadAnimation(context, R.anim.exit_to_bottom)
        bottomAnimation.setAnimationListener(
            object : SimpleAnimationListener() {
                override fun onAnimationEnd(animation: Animation) {
                    common_action_toolbar.isVisible = false
                }
            }
        )
        common_action_toolbar.startAnimation(bottomAnimation)
    }
}
