package eu.kanade.tachiyomi.ui.setting.track

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackLogoutDialog(bundle: Bundle? = null) : DialogController(bundle) {

    private val service = Injekt.get<TrackManager>().getService(args.getInt("serviceId"))!!

    constructor(service: TrackService) : this(bundleOf("serviceId" to service.id))

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val serviceName = activity!!.getString(service.nameRes())
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(activity!!.getString(R.string.logout_title, serviceName))
            .setPositiveButton(R.string.logout) { _, _ ->
                service.logout()
                (targetController as? Listener)?.trackLogoutDialogClosed(service)
                activity?.toast(R.string.logout_success)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    interface Listener {
        fun trackLogoutDialogClosed(service: TrackService)
    }
}
