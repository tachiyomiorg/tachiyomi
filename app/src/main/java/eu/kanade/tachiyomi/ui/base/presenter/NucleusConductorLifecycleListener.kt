package eu.kanade.tachiyomi.ui.base.presenter

import android.os.Bundle
import android.view.View
import com.bluelinelabs.conductor.Controller

class NucleusConductorLifecycleListener(private val delegate: NucleusConductorDelegate<*>) : Controller.LifecycleListener() {

    override fun postCreateView(controller: Controller, view: View) {
        delegate.onTakeView(controller)
    }

    override fun preDestroyView(controller: Controller, view: View) {
        delegate.onDropView()
    }

    override fun preDestroy(controller: Controller) {
        delegate.onDestroy()
    }

    override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
        outState.putBundle(PRESENTER_STATE_KEY, delegate.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
        delegate.onRestoreInstanceState(savedInstanceState.getBundle(PRESENTER_STATE_KEY))
    }

    companion object {
        private const val PRESENTER_STATE_KEY = "presenter_state"
    }
}
