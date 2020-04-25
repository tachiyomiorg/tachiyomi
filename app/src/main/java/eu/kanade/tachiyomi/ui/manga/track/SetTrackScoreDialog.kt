package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
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

class SetTrackScoreDialog<T> : DialogController
        where T : Controller, T : SetTrackScoreDialog.Listener {

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
            .title(R.string.score)
            .customView(R.layout.track_score_dialog, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) { dialog ->
                val view = dialog.getCustomView()
                // Remove focus to update selected number
                val np: NumberPicker = view.findViewById(R.id.score_picker)
                np.clearFocus()

                (targetController as? Listener)?.setScore(item, np.value)
            }
            .negativeButton(android.R.string.cancel)

        val view = dialog.getCustomView()
        val np: NumberPicker = view.findViewById(R.id.score_picker)
        val scores = item.service.getScoreList().toTypedArray()
        np.maxValue = scores.size - 1
        np.displayedValues = scores

        // Set initial value
        val displayedScore = item.service.displayScore(item.track!!)
        if (displayedScore != "-") {
            val index = scores.indexOf(displayedScore)
            np.value = if (index != -1) index else 0
        }

        return dialog
    }

    interface Listener {
        fun setScore(item: TrackItem, score: Int)
    }

    private companion object {
        const val KEY_ITEM_TRACK = "SetTrackScoreDialog.item.track"
    }
}
