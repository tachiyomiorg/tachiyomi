package eu.kanade.tachiyomi.ui.library

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.category.CategoryController

class ChangeMangaCategoriesDialog<T>(bundle: Bundle? = null) :
    DialogController(bundle) where T : Controller, T : ChangeMangaCategoriesDialog.Listener {

    private var mangas = emptyList<Manga>()
    private var categories = emptyList<Category>()
    private var preselected = emptyArray<Int>()

    constructor(
        target: T,
        mangas: List<Manga>,
        categories: List<Category>,
        preselected: Array<Int>
    ) : this() {
        this.mangas = mangas
        this.categories = categories
        this.preselected = preselected
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.action_move_category)
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (categories.isNotEmpty()) {
                    val selected = categories
                        .mapIndexed { i, _ -> preselected.contains(i) }
                        .toBooleanArray()
                    setMultiChoiceItems(categories.map { it.name }.toTypedArray(), selected) { _, which, checked ->
                        selected[which] = checked
                    }
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        val newCategories = categories.filterIndexed { i, _ -> selected[i] }
                        (targetController as? Listener)?.updateCategoriesForMangas(mangas, newCategories)
                    }
                } else {
                    setMessage(R.string.information_empty_category_dialog)
                    setPositiveButton(R.string.action_edit_categories) { _, _ ->
                        if (targetController is LibraryController) {
                            val libController = targetController as LibraryController
                            libController.clearSelection()
                        }
                        router.popCurrentController()
                        router.pushController(CategoryController().withFadeTransaction())
                    }
                }
            }
            .create()
    }

    interface Listener {
        fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>)
    }
}
