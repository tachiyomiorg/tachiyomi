package eu.kanade.tachiyomi.ui.library

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.action_edit_cover)
            .setPositiveButton(R.string.action_edit) { _, _ ->
                (targetController as? Listener)?.openMangaCoverPicker(manga)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.action_delete) { _, _ ->
                (targetController as? Listener)?.deleteMangaCover(manga)
            }
            .create()
    }

    interface Listener {
        fun deleteMangaCover(manga: Manga)

        fun openMangaCoverPicker(manga: Manga)
    }
}
