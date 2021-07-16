package eu.kanade.tachiyomi.ui.recent.history

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.widget.DialogCheckboxView

class RemoveHistoryDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : RemoveHistoryDialog.Listener {

    private var manga: Manga? = null

    private var history: History? = null

    constructor(target: T, manga: Manga, history: History) : this() {
        this.manga = manga
        this.history = history
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val activity = activity!!

        // Create custom view
        val dialogCheckboxView = DialogCheckboxView(activity).apply {
            setDescription(R.string.dialog_with_checkbox_remove_description)
            setOptionDescription(R.string.dialog_with_checkbox_reset)
        }

        return MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.action_remove)
            .setView(dialogCheckboxView)
            .setPositiveButton(R.string.action_remove) { _, _ -> onPositive(dialogCheckboxView.isChecked()) }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun onPositive(checked: Boolean) {
        val target = targetController as? Listener ?: return
        val manga = manga ?: return
        val history = history ?: return

        target.removeHistory(manga, history, checked)
    }

    interface Listener {
        fun removeHistory(manga: Manga, history: History, all: Boolean)
    }
}
