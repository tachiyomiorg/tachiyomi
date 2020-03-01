package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SetTrackStatusDialog<T> : DialogController
        where T : Controller, T : SetTrackStatusDialog.Listener {

    private val item: TrackItem

    constructor(target: T, item: TrackItem) : super(Bundle().apply {
        putSerializable(KEY_ITEM_TRACK, item.track)
    }) {
        targetController = target
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
        val statusString = statusList.mapNotNull { item.service.getStatus(it) }
        val selectedIndex = statusList.indexOf(item.track?.status)

        return MaterialDialog.Builder(activity!!)
                .title(R.string.status)
                .negativeText(android.R.string.cancel)
                .items(statusString)
                .itemsCallbackSingleChoice(selectedIndex) { _, _, i, _ ->
                    (targetController as? Listener)?.setStatus(item, i)
                    true
                }
                .build()
    }

    interface Listener {
        fun setStatus(item: TrackItem, selection: Int)
    }

    private companion object {
        const val KEY_ITEM_TRACK = "SetTrackStatusDialog.item.track"
    }
}
