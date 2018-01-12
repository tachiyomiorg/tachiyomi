package eu.kanade.tachiyomi.ui.manga.chapter

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController


class DownloadCustomChaptersDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : DownloadChaptersDialog.Listener {

    constructor(target: T) : this() {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val activity = activity!!

        return MaterialDialog.Builder(activity)
                .title(R.string.custom_download)
                .inputRange(1, 3)
                .negativeText(android.R.string.cancel)
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .input(
                        activity.getString(R.string.custom_hint),
                        "",
                        false,
                        { _, input -> (targetController as? Listener)?.downloadCustomChapters(Integer.parseInt(input.toString())) })
                .build()
    }

    interface Listener {
        fun downloadCustomChapters(amount: Int)
    }

}