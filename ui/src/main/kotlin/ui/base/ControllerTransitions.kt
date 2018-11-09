package tachiyomi.ui.base

import android.transition.Fade
import android.transition.Transition
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import ui.base.SimpleTransitionChangeHandler

fun Controller.withHorizontalTransition(): RouterTransaction {
  return RouterTransaction.with(this)
    .pushChangeHandler(HorizontalChangeHandler())
    .popChangeHandler(HorizontalChangeHandler())
}

fun Controller.withFadeTransition(): RouterTransaction {
  return RouterTransaction.with(this)
    .pushChangeHandler(FadeTransition())
    .popChangeHandler(FadeTransition())
}

fun Controller.withoutTransition(): RouterTransaction {
  return RouterTransaction.with(this)
}

class FadeTransition : SimpleTransitionChangeHandler() {

  override fun getExitTransition(
    container: ViewGroup,
    from: View?,
    to: View?,
    isPush: Boolean
  ): Transition? {
    return Fade().excludeTarget(Toolbar::class.java, true)
  }

  override fun getEnterTransition(
    container: ViewGroup,
    from: View?,
    to: View?,
    isPush: Boolean
  ): Transition? {
    return Fade().excludeTarget(Toolbar::class.java, true)
  }

}
