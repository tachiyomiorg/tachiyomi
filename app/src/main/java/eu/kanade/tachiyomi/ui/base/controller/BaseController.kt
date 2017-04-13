package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller

abstract class BaseController(bundle: Bundle? = null) : Controller(bundle) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflateView(inflater, container)
        onViewCreated(view)
        return view
    }

    abstract fun inflateView(inflater: LayoutInflater, container: ViewGroup): View

    open fun onViewCreated(view: View) { }

    override fun onAttach(view: View) {
        setTitle()
        super.onAttach(view)
    }

    open fun getTitle(): String? {
        return null
    }

    private fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController && parentController.getTitle() != null) {
                return
            }
            parentController = parentController.parentController
        }

        (activity as? AppCompatActivity)?.supportActionBar?.title = getTitle()
    }

    inline fun withView(block: View.() -> Unit) {
        view?.let { block(it) }
    }

}