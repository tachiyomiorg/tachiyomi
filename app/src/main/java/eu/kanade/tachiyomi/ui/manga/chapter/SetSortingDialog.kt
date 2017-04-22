package eu.kanade.tachiyomi.ui.manga.chapter

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class SetSortingDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : SetSortingDialog.Listener {

    private val selectedIndex = args.getInt("selected", -1)

    constructor(target: T, selectedIndex: Int = -1) : this(Bundle().apply {
        putInt("selected", selectedIndex)
    }) {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val activity = activity!!
        val ids = intArrayOf(Manga.SORTING_SOURCE, Manga.SORTING_NUMBER)
        val choices = intArrayOf(R.string.sort_by_source, R.string.sort_by_number)
                .map { activity.getString(it) }

        return MaterialDialog.Builder(activity)
                .title(R.string.sorting_mode)
                .items(choices)
                .itemsIds(ids)
                .itemsCallbackSingleChoice(selectedIndex) { _, itemView, _, _ ->
                    (targetController as? Listener)?.setSorting(itemView.id)
                    true
                }
                .build()
    }

    interface Listener {
        fun setSorting(id: Int)
    }

}