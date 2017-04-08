package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.bluelinelabs.conductor.Controller

abstract class BaseController(bundle: Bundle? = null) : Controller(bundle) {

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

}