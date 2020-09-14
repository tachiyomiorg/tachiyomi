package eu.kanade.tachiyomi.ui.base.controller

import android.animation.Animator
import android.animation.AnimatorSet
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler

/**
 * A variation of [FadeChangeHandler] that only fades in.
 */
class OneWayFadeChangeHandler : FadeChangeHandler {
    constructor()
    constructor(removesFromViewOnPush: Boolean) : super(removesFromViewOnPush)
    constructor(duration: Long) : super(duration)
    constructor(duration: Long, removesFromViewOnPush: Boolean) : super(
        duration,
        removesFromViewOnPush
    )

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animator {
        if (to != null) {
            return super.getAnimator(container, from, to, isPush, toAddedToContainer)
        }

        if (from != null && (!isPush || removesFromViewOnPush())) {
            container.removeView(from)
        }

        return AnimatorSet()
    }

    override fun copy(): ControllerChangeHandler {
        return OneWayFadeChangeHandler(animationDuration, removesFromViewOnPush())
    }
}
