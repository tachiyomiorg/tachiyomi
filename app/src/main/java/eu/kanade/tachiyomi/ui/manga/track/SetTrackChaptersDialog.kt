package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SetTrackChaptersDialog<T> : DialogController
        where T : Controller, T : SetTrackChaptersDialog.Listener {

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

        val dialog = MaterialDialog.Builder(activity!!)
                .title(R.string.chapters)
                .customView(R.layout.track_chapters_dialog, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { dialog, _ ->
                    val view = dialog.customView
                    if (view != null) {
                        // Remove focus to update selected number
                        val np: NumberPicker = view.findViewById(R.id.chapters_picker)
                        np.clearFocus()

                        (targetController as? Listener)?.setChaptersRead(item, np.value)
                    }
                }
                .build()

        val view = dialog.customView
        if (view != null) {
            val np: NumberPicker = view.findViewById(R.id.chapters_picker)
            // Set initial value
            np.value = item.track?.last_chapter_read ?: 0
            // Don't allow to go from 0 to 9999
            np.wrapSelectorWheel = false
        }

        return dialog
    }

    interface Listener {
        fun setChaptersRead(item: TrackItem, chaptersRead: Int)
    }

    private companion object {
        const val KEY_ITEM_TRACK = "SetTrackChaptersDialog.item.track"
    }
}
