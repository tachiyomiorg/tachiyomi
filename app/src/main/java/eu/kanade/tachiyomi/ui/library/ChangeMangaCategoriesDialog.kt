package eu.kanade.tachiyomi.ui.library

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.bluelinelabs.conductor.Controller
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
        return MaterialDialog(activity!!)
            .title(R.string.action_move_category)
            .negativeButton(android.R.string.cancel)
            .apply {
                if (categories.isNotEmpty()) {
                    listItemsMultiChoice(
                        items = categories.map { it.name },
                        initialSelection = preselected.toIntArray(),
                        allowEmptySelection = true
                    ) { _, selections, _ ->
                        val newCategories = selections.map { categories[it] }
                        (targetController as? Listener)?.updateCategoriesForMangas(mangas, newCategories)
                    }
                        .positiveButton(android.R.string.ok)
                } else {
                    message(R.string.information_empty_category_dialog)
                        .positiveButton(R.string.action_edit_categories) {
                            if (targetController is LibraryController) {
                                val libController = targetController as LibraryController
                                libController.clearSelection()
                            }
                            router.popCurrentController()
                            router.pushController(CategoryController().withFadeTransaction())
                        }
                }
            }
    }

    interface Listener {
        fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>)
    }
}
