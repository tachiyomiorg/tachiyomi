package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.toast
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackLogoutDialog(bundle: Bundle? = null) : DialogController(bundle) {

    private val service = Injekt.get<TrackManager>().getService(args.getInt("key"))!!

    constructor(service: TrackService) : this(Bundle().apply { putInt("key", service.id) })

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog.Builder(activity!!)
                .title(activity!!.getString(R.string.logout_title, service.name))
                .positiveText(R.string.logout)
                .onPositive { _, _ ->
                    service.logout()
                    (targetController as? Listener)?.trackLogoutDialogClosed(service)
                    activity?.toast(R.string.logout_success)
                }
                .negativeText(android.R.string.cancel)
                .build()
    }

    interface Listener {
        fun trackLogoutDialogClosed(service: TrackService)
    }

}
