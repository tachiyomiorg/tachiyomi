package eu.kanade.tachiyomi.ui.library

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class ChangeMangaCoverDialog<T>(bundle: Bundle? = null) :
    DialogController(bundle) where T : Controller, T : ChangeMangaCoverDialog.Listener {

    private lateinit var manga: Manga

    constructor(target: T, manga: Manga) : this() {
        targetController = target
        this.manga = manga
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(R.string.action_edit_cover)
            .positiveButton(R.string.action_edit) {
                (targetController as? Listener)?.openMangaCoverPicker(manga)
            }
            .negativeButton(android.R.string.cancel)
            .neutralButton(R.string.action_delete) {
                (targetController as? Listener)?.deleteMangaCover(manga)
            }
    }

    interface Listener {
        fun deleteMangaCover(manga: Manga)

        fun openMangaCoverPicker(manga: Manga)
    }
}
