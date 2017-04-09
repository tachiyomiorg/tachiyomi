package eu.kanade.tachiyomi.ui.category

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class CategoryRenameDialog(bundle: Bundle) : DialogController(bundle) {

    private lateinit var category: Category

    private var currentName = ""

    constructor(category: Category, categoryController: CategoryController) : this(Bundle()) {
        this.category = category
        targetController = categoryController
        currentName = category.name
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        router.popController(this)
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog.Builder(activity!!)
                .title(R.string.action_rename_category)
                .negativeText(android.R.string.cancel)
                .alwaysCallInputCallback()
                .input(resources!!.getString(R.string.name), currentName, false, { _, input ->
                    currentName = input.toString()
                })
                .onPositive { _, _ -> onPositive() }
                .build()
    }

    private fun onPositive() {
        val target = targetController as? CategoryController ?: return
        target.presenter.renameCategory(category, currentName)
    }

}