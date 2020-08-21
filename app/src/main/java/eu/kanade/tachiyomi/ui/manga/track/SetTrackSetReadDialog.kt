package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SetTrackSetReadDialog<T> : DialogController
        where T : Controller, T : SetTrackSetReadDialog.Listener {

    private val item: TrackItem

    constructor(target: T, item: TrackItem) : super(
        Bundle().apply {
            putSerializable(KEY_ITEM_TRACK, item.track)
        }
    ) {
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

        val dialog = MaterialDialog(activity!!)
            .title(R.string.import_read)
            .customView(R.layout.track_set_read_dialog, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) { _ ->

                val latestTrackedChapter = item.track?.last_chapter_read!!

                (targetController as? Listener)?.setRead(latestTrackedChapter)
            }
            .negativeButton(android.R.string.cancel)

        val view = dialog.getCustomView()
        val txtView: TextView = view.findViewById(R.id.set_read_confirm)

        // Set contents of set_read_confirm to include current last read from tracking
        val confirmMsg = "Mark " + (item.track?.last_chapter_read) + " chapters read?"
        // apply to text view
        txtView.text = confirmMsg

        return dialog
    }

    interface Listener {
        fun setRead(latestTrackedChapter: Int)
    }

    private companion object {
        const val KEY_ITEM_TRACK = "SetTrackSetReadDialog.item.track"
    }
}
