package eu.kanade.tachiyomi.ui.category

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.controller.DialogController

/**
 * Dialog to rename an existing category of the library.
 */
class CategoryRenameDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : CategoryRenameDialog.Listener {

    private var category: Category? = null

    /**
     * Name of the new category. Value updated with each input from the user.
     */
    private var currentName = ""

    constructor(target: T, category: Category) : this() {
        targetController = target
        this.category = category
        currentName = category.name
    }

    /**
     * Called when creating the dialog for this controller.
     *
     * @param savedViewState The saved state of this dialog.
     * @return a new dialog instance.
     */
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(R.string.action_rename_category)
            .negativeButton(android.R.string.cancel)
            .input(
                hint = resources?.getString(R.string.name),
                prefill = currentName
            ) { _, input ->
                currentName = input.toString()
            }
            .positiveButton(android.R.string.ok) { onPositive() }
    }

    /**
     * Called to save this Controller's state in the event that its host Activity is destroyed.
     *
     * @param outState The Bundle into which data should be saved
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(CATEGORY_KEY, category)
        super.onSaveInstanceState(outState)
    }

    /**
     * Restores data that was saved in the [onSaveInstanceState] method.
     *
     * @param savedInstanceState The bundle that has data to be restored
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        category = savedInstanceState.getSerializable(CATEGORY_KEY) as? Category
    }

    /**
     * Called when the positive button of the dialog is clicked.
     */
    private fun onPositive() {
        val target = targetController as? Listener ?: return
        val category = category ?: return

        target.renameCategory(category, currentName)
    }

    interface Listener {
        fun renameCategory(category: Category, name: String)
    }

    private companion object {
        const val CATEGORY_KEY = "CategoryRenameDialog.category"
    }
}
