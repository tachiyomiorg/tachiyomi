package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.RestoreViewOnCreateController
import com.bluelinelabs.conductor.Router

abstract class BaseController(bundle: Bundle? = null) : RestoreViewOnCreateController(bundle) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflateView(inflater, container)
        onViewCreated(view, savedViewState)
        return view
    }

    abstract fun inflateView(inflater: LayoutInflater, container: ViewGroup): View

    open fun onViewCreated(view: View, savedViewState: Bundle?) { }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        if (type.isEnter) {
            setTitle()
        }
        super.onChangeStarted(handler, type)
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

    fun Router.popControllerWithTag(tag: String): Boolean {
        val controller = getControllerWithTag(tag)
        if (controller != null) {
            popController(controller)
            return true
        }
        return false
    }

}