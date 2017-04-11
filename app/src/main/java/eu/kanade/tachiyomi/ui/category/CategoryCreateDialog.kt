package eu.kanade.tachiyomi.ui.category

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class CategoryCreateDialog(bundle: Bundle) : DialogController(bundle) {

    private var currentName = ""

    constructor(categoryController: CategoryController) : this(Bundle()) {
        targetController = categoryController
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog.Builder(activity!!)
                .title(R.string.action_add_category)
                .negativeText(android.R.string.cancel)
                .alwaysCallInputCallback()
                .input(resources?.getString(R.string.name), currentName, false, { _, input ->
                    currentName = input.toString()
                })
                .onPositive { _, _ -> onPositive() }
                .build()
    }

    private fun onPositive() {
        val target = targetController as? CategoryController ?: return
        target.presenter.createCategory(currentName)
    }

}