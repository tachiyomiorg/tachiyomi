package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import androidx.core.os.bundleOf
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

class SetTrackChaptersDialog<T> : DialogController
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

        val dialog = MaterialDialog(activity!!)
            .title(R.string.chapters)
            .customView(R.layout.track_chapters_dialog, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) { dialog ->
                val view = dialog.getCustomView()
                // Remove focus to update selected number
                val np: NumberPicker = view.findViewById(R.id.chapters_picker)
                np.clearFocus()

                listener.setChaptersRead(item, np.value)
            }
            .negativeButton(android.R.string.cancel)

        val view = dialog.getCustomView()
        val np: NumberPicker = view.findViewById(R.id.chapters_picker)
        // Set initial value
        np.value = item.track?.last_chapter_read ?: 0

        // Enforce maximum value if tracker has total number of chapters set
        if (item.track != null && item.track.total_chapters > 0) {
            np.maxValue = item.track.total_chapters
        }

        // Don't allow to go from 0 to 9999
        np.wrapSelectorWheel = false

        return dialog
    }

    interface Listener {
        fun setChaptersRead(item: TrackItem, chaptersRead: Int)
    }

    private companion object {
        const val KEY_ITEM_TRACK = "SetTrackChaptersDialog.item.track"
    }
}
