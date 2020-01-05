package eu.kanade.tachiyomi.ui.library

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.widget.DialogCheckboxView

class DeleteLibraryMangasDialog<T>(bundle: Bundle? = null) :
        DialogController(bundle) where T : Controller, T: DeleteLibraryMangasDialog.Listener {

    private var mangas = emptyList<Manga>()

    constructor(target: T, mangas: List<Manga>) : this() {
        this.mangas = mangas
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val view = DialogCheckboxView(activity!!).apply {
            setDescription(R.string.confirm_delete_manga)
            setOptionDescription(R.string.also_delete_chapters)
        }

        return MaterialDialog.Builder(activity!!)
                .title(R.string.action_remove)
                .customView(view, true)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive { _, _ ->
                    val deleteChapters = view.isChecked()
                    (targetController as? Listener)?.deleteMangasFromLibrary(mangas, deleteChapters)
                }
                .build()
    }

    interface Listener {
        fun deleteMangasFromLibrary(mangas: List<Manga>, deleteChapters: Boolean)
    }
}