package eu.kanade.tachiyomi.ui.library

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class ReadLibraryMangasDialog<T>(bundle: Bundle? = null) :
    DialogController(bundle) where T : Controller, T : ReadLibraryMangasDialog.Listener {

    private var mangas = emptyList<Manga>()
    private var read: Boolean = true

    constructor(target: T, mangas: List<Manga>, read: Boolean) : this() {
        this.mangas = mangas
        this.read = read
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(if (read) R.string.action_mark_as_read else R.string.action_mark_as_unread)
            .positiveButton(android.R.string.ok) {
                (targetController as? Listener)?.markReadStatus(mangas, read)
            }
            .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun markReadStatus(mangas: List<Manga>, read: Boolean)
    }
}
