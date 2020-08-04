package eu.kanade.tachiyomi.ui.main

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class WhatsNewDialogController(bundle: Bundle? = null) : DialogController(bundle) {

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(text = activity!!.getString(R.string.updated_version, BuildConfig.VERSION_NAME))
            .positiveButton(android.R.string.ok)
            .neutralButton(R.string.whats_new) {
                val url = "https://github.com/inorichi/tachiyomi/releases/tag/v${BuildConfig.VERSION_NAME}"
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
            }
    }
}
