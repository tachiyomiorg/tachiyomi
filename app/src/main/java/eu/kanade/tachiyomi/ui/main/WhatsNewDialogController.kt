package eu.kanade.tachiyomi.ui.main

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.openInBrowser

class WhatsNewDialogController(bundle: Bundle? = null) : DialogController(bundle) {

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(text = activity!!.getString(R.string.updated_version, BuildConfig.VERSION_NAME))
            .positiveButton(android.R.string.ok)
            .neutralButton(R.string.whats_new) {
                openInBrowser("https://github.com/tachiyomiorg/tachiyomi/releases/tag/v${BuildConfig.VERSION_NAME}")
            }
    }
}
