package eu.kanade.tachiyomi.ui.manga.chapter

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.widget.DialogCustomDownloadView

/**
 * Dialog used to let user select amount of chapters to download.
 */
class DownloadCustomChaptersDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : DownloadCustomChaptersDialog.Listener {

    private var maxChapters = 0

    /**
     * Initialize dialog.
     * @param maxChapters maximal number of chapters that user can download.
     */
    constructor(target: T, maxChapters: Int) : this() {
        this.maxChapters = maxChapters
        targetController = target
    }

    /**
     * Called when dialog is being created.
     */
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val activity = activity!!

        // Initialize view that lets user select number of chapters to download.
        val view = DialogCustomDownloadView(activity).apply {
            setMinMax(0, maxChapters)
        }

        // Build dialog.
        // when positive dialog is pressed call custom listener.
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