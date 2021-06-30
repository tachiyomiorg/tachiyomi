package eu.kanade.tachiyomi.ui.manga.info

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class ChangeMangaPublicationStatusDialog<T>(bundle: Bundle? = null) :
    DialogController(bundle) where T : Controller, T : ChangeMangaPublicationStatusDialog.Listener {

    private var realStatus: Int = SManga.UNKNOWN
    private var overrideStatus: Int = SManga.UNKNOWN

    constructor(
        target: T,
        realStatus: Int,
        overrideStatus: Int
    ) : this() {
        targetController = target
        this.realStatus = realStatus
        this.overrideStatus = overrideStatus
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(R.string.set_manga_status)
            .listItemsSingleChoice(
                res = R.array.manga_override_status_options,
                initialSelection = overrideStatus,
            ) { _, index, _ ->
                (targetController as? Listener)?.updatePublicationStatusForManga(realStatus, index)
            }
            .positiveButton(android.R.string.ok)
            .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun updatePublicationStatusForManga(sourceStatus: Int, overrideStatus: Int)
    }
}
