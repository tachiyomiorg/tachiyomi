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
import eu.kanade.tachiyomi.databinding.CommonActionToolbarBinding

/**
 * A toolbar holding only menu items.
 */
class ActionToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val binding: CommonActionToolbarBinding

    init {
        binding = CommonActionToolbarBinding.inflate(LayoutInflater.from(context), this, true)
    }

    /**
     * Remove menu items and remove listener.
     */
    fun destroy() {
        binding.commonActionMenu.menu.clear()
        binding.commonActionMenu.setOnMenuItemClickListener(null)
    }

    /**
     * Gets a menu item if found.
     */
    fun findItem(@IdRes itemId: Int): MenuItem? {
        return binding.commonActionMenu.menu.findItem(itemId)
    }

    /**
     * Show the menu toolbar using the provided ActionMode's context to inflate the items.
     */
    fun show(mode: ActionMode, @MenuRes menuRes: Int, listener: (item: MenuItem?) -> Boolean) {
        // Avoid re-inflating the menu
        if (binding.commonActionMenu.menu.size() == 0) {
            mode.menuInflater.inflate(menuRes, binding.commonActionMenu.menu)
            binding.commonActionMenu.setOnMenuItemClickListener { listener(it) }
        }

        binding.commonActionToolbar.isVisible = true
        val bottomAnimation = AnimationUtils.loadAnimation(context, R.anim.enter_from_bottom)
        binding.commonActionToolbar.startAnimation(bottomAnimation)
    }

    /**
     * Hide the menu toolbar.
     */
    fun hide() {
        val bottomAnimation = AnimationUtils.loadAnimation(context, R.anim.exit_to_bottom)
        bottomAnimation.setAnimationListener(
            object : SimpleAnimationListener() {
                override fun onAnimationEnd(animation: Animation) {
                    binding.commonActionToolbar.isVisible = false
                }
            }
        )
        binding.commonActionToolbar.startAnimation(bottomAnimation)
    }
}
