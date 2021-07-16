package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.TrackChaptersDialogBinding
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
        val pickerView = TrackChaptersDialogBinding.inflate(LayoutInflater.from(activity!!))
        val np = pickerView.chaptersPicker

        // Set initial value
        np.value = item.track?.last_chapter_read ?: 0

        // Enforce maximum value if tracker has total number of chapters set
        if (item.track != null && item.track.total_chapters > 0) {
            np.maxValue = item.track.total_chapters
        }

        // Don't allow to go from 0 to 9999
        np.wrapSelectorWheel = false

        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.chapters)
            .setView(pickerView.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                np.clearFocus()
                listener.setChaptersRead(item, np.value)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    interface Listener {
        fun setChaptersRead(item: TrackItem, chaptersRead: Int)
    }

    private companion object {
        const val KEY_ITEM_TRACK = "SetTrackChaptersDialog.item.track"
    }
}
