package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SetTrackStatusDialog<T> : DialogController
        where T : Controller {

    private val item: TrackItem

    private lateinit var listener: Listener

    constructor(target: T, listener: Listener, item: TrackItem) : super(
        bundleOf(KEY_ITEM_TRACK to item.track)
    ) {
        targetController = target
        this.listener = listener
        this.item = item
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        val track = bundle.getSerializable(KEY_ITEM_TRACK) as Track
        val service = Injekt.get<TrackManager>().getService(track.sync_id)!!
        item = TrackItem(track, service)
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val item = item
        val statusList = item.service.getStatusList()
        val statusString = statusList.map { item.service.getStatus(it) }
        val selectedIndex = statusList.indexOf(item.track?.status)

        return MaterialDialog(activity!!)
            .title(R.string.status)
            .negativeButton(android.R.string.cancel)
            .listItemsSingleChoice(
                items = statusString,
                initialSelection = selectedIndex,
                waitForPositiveButton = false
            ) { dialog, position, _ ->
                listener.setStatus(item, position)
                dialog.dismiss()
            }
    }

    interface Listener {
        fun setStatus(item: TrackItem, selection: Int)
    }

    private companion object {
        const val KEY_ITEM_TRACK = "SetTrackStatusDialog.item.track"
    }
}
