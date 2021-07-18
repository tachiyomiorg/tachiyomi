package eu.kanade.tachiyomi.ui.manga.chapter

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class DeleteChaptersDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : DeleteChaptersDialog.Listener {

    constructor(target: T) : this() {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(activity!!)
            .setMessage(R.string.confirm_delete_chapters)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                (targetController as? Listener)?.deleteChapters()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    interface Listener {
        fun deleteChapters()
    }
}
