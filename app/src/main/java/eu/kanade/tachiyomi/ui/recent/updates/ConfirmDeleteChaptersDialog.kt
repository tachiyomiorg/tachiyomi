package eu.kanade.tachiyomi.ui.recent.updates

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class ConfirmDeleteChaptersDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : ConfirmDeleteChaptersDialog.Listener {

    private var chaptersToDelete = emptyList<UpdatesItem>()

    constructor(target: T, chaptersToDelete: List<UpdatesItem>) : this() {
        this.chaptersToDelete = chaptersToDelete
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(activity!!)
            .setMessage(R.string.confirm_delete_chapters)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                (targetController as? Listener)?.deleteChapters(chaptersToDelete)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    interface Listener {
        fun deleteChapters(chaptersToDelete: List<UpdatesItem>)
    }
}
