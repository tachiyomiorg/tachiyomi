package eu.kanade.tachiyomi.ui.category

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

/**
 * Dialog to create a new category for the library.
 */
class CategoryCreateDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : CategoryCreateDialog.Listener {

    /**
     * Name of the new category. Value updated with each input from the user.
     */
    private var currentName = ""

    constructor(target: T) : this() {
        targetController = target
    }

    /**
     * Called when creating the dialog for this controller.
     *
     * @param savedViewState The saved state of this dialog.
     * @return a new dialog instance.
     */
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(R.string.action_add_category)
            .negativeButton(android.R.string.cancel)
            .input(
                hint = resources?.getString(R.string.name),
                prefill = currentName
            ) { _, input ->
                currentName = input.toString()
            }
            .positiveButton(android.R.string.ok) {
                (targetController as? Listener)?.createCategory(currentName)
            }
    }

    interface Listener {
        fun createCategory(name: String)
    }
}
