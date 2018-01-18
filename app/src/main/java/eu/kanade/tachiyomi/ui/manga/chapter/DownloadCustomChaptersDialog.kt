package eu.kanade.tachiyomi.ui.manga.chapter

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.widget.DialogCustomDownloadView


class DownloadCustomChaptersDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : DownloadCustomChaptersDialog.Listener {

    private var maxChapters = 10

    constructor(target: T, maxChapters: Int) : this() {
        this.maxChapters = maxChapters
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val activity = activity!!

        val view = DialogCustomDownloadView(activity).apply {
            setMinMax(0, maxChapters)
        }

        return MaterialDialog.Builder(activity)
                .title(R.string.custom_download)
                .customView(view, true)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { _, _ ->
                    (targetController as? Listener)?.downloadCustomChapters(view.amount)
                }
                .build()
    }

    interface Listener {
        fun downloadCustomChapters(amount: Int)
    }


}