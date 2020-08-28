package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
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
        var latestTrackedChapter: Int = item.track?.last_chapter_read!!
        val lastSourceChapter: Float = sourceChapters[0].chapter_number
        val confirmMsg: String

        // get last chapter number published to source
        if (latestTrackedChapter > lastSourceChapter) {
            latestTrackedChapter = lastSourceChapter.toInt()
            // display message to advise why latestTrackedChapter is now lower
            confirmMsg = this.resources!!.getString(R.string.sync_confirm_tracker_ahead, latestTrackedChapter)
        } else {
            confirmMsg = this.resources!!.getString(R.string.sync_confirm, latestTrackedChapter)
        }

        return MaterialDialog(activity!!)
            .message(text = confirmMsg)
            .title(R.string.sync_chapters)
            .positiveButton(android.R.string.ok) {
                (targetController as? Listener)?.getChaptersRead(latestTrackedChapter)
            }
            .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun getChaptersRead(latestTrackedChapter: Int)
    }

    private companion object {
        const val KEY_ITEM_TRACK = "GetTrackChaptersDialog.item.track"
    }
}