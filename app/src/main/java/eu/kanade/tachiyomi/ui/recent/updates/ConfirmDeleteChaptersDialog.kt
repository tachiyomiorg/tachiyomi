package eu.kanade.tachiyomi.ui.recent.updates

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
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
        return MaterialDialog(activity!!)
            .message(R.string.confirm_delete_chapters)
            .positiveButton(android.R.string.ok) {
                (targetController as? Listener)?.deleteChapters(chaptersToDelete)
            }
            .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun deleteChapters(chaptersToDelete: List<UpdatesItem>)
    }
}
