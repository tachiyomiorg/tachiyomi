package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ActionToolbarBinding
import eu.kanade.tachiyomi.widget.listener.SimpleAnimationListener

/**
 * A toolbar holding only menu items.
 */
class ActionToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val binding = ActionToolbarBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * Remove menu items and remove listener.
     */
    fun destroy() {
        binding.menu.menu.clear()
        binding.menu.setOnMenuItemClickListener(null)
    }

    /**
     * Gets a menu item if found.
     */
    fun findItem(@IdRes itemId: Int): MenuItem? {
        return binding.menu.menu.findItem(itemId)
    }

    /**
     * Show the menu toolbar using the provided ActionMode's context to inflate the items.
     */
    fun show(mode: ActionMode, @MenuRes menuRes: Int, listener: (item: MenuItem?) -> Boolean) {
        // Avoid re-inflating the menu
        if (binding.menu.menu.size() == 0) {
            mode.menuInflater.inflate(menuRes, binding.menu.menu)
            binding.menu.setOnMenuItemClickListener { listener(it) }
        }

        binding.actionToolbar.isVisible = true
        val bottomAnimation = AnimationUtils.loadAnimation(context, R.anim.enter_from_bottom)
        binding.actionToolbar.startAnimation(bottomAnimation)
    }

    /**
     * Hide the menu toolbar.
     */
    fun hide() {
        val bottomAnimation = AnimationUtils.loadAnimation(context, R.anim.exit_to_bottom)
        bottomAnimation.setAnimationListener(
            object : SimpleAnimationListener() {
                override fun onAnimationEnd(animation: Animation) {
                    binding.actionToolbar.isVisible = false
                }
            }
        )
        binding.actionToolbar.startAnimation(bottomAnimation)
    }
}
