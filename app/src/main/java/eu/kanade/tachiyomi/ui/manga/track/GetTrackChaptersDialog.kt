package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GetTrackChaptersDialog<T> : DialogController
        where T : Controller, T : GetTrackChaptersDialog.Listener {

    private val item: TrackItem

    private var sourceChapters = emptyList<Chapter>()

    constructor(target: T, item: TrackItem, chapters: List<Chapter>) : super(
        Bundle().apply {
            putSerializable(KEY_ITEM_TRACK, item.track)
        }
    ) {
        targetController = target
        this.item = item
        sourceChapters = chapters
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        val track = bundle.getSerializable(KEY_ITEM_TRACK) as Track
        val service = Injekt.get<TrackManager>().getService(track.sync_id)!!
        item = TrackItem(track, service)
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val item = item
        var latestTrackedChapter = item.track?.last_chapter_read!!
        val dialog = MaterialDialog(activity!!)
            .title(R.string.sync_chapters)
            .customView(R.layout.track_get_chapters_dialog, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) { _ ->
                (targetController as? Listener)?.getChaptersRead(latestTrackedChapter)
            }
            .negativeButton(android.R.string.cancel)

        val view = dialog.getCustomView()
        val txtView: TextView = view.findViewById(R.id.get_track_chapters_confirm)
        val confirmMsg: String

        // get last chapter number published to source
        val lastSourceChapter = sourceChapters[0].chapter_number

        // check if tracker last chapter ahead of source last chapter
        if (latestTrackedChapter > lastSourceChapter) {
            // set latestTrackedChapter to the latest source chapter
            latestTrackedChapter = lastSourceChapter.toInt()

            // display message to advise why latestTrackedChapter is now lower
            confirmMsg = "Tracker is ahead of source!\n\nMark read up to chapter $latestTrackedChapter?"
        } else {
            confirmMsg = "Mark read up to chapter $latestTrackedChapter?"
        }
        // apply context message to text view
        txtView.text = confirmMsg

        return dialog
    }

    interface Listener {
        fun getChaptersRead(latestTrackedChapter: Int)
    }

    private companion object {
        const val KEY_ITEM_TRACK = "SetTrackSetReadDialog.item.track"
    }
}
