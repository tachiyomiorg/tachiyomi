package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import android.view.View
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.track_item.logo_container
import kotlinx.android.synthetic.main.track_item.track_chapters
import kotlinx.android.synthetic.main.track_item.track_details
import kotlinx.android.synthetic.main.track_item.track_logo
import kotlinx.android.synthetic.main.track_item.track_score
import kotlinx.android.synthetic.main.track_item.track_set
import kotlinx.android.synthetic.main.track_item.track_status
import kotlinx.android.synthetic.main.track_item.track_title

class TrackHolder(view: View, adapter: TrackAdapter) : BaseViewHolder(view) {

    init {
        val listener = adapter.rowClickListener

        logo_container.setOnClickListener { listener.onLogoClick(bindingAdapterPosition) }
        track_set.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        track_title.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        track_status.setOnClickListener { listener.onStatusClick(bindingAdapterPosition) }
        track_chapters.setOnClickListener { listener.onChaptersClick(bindingAdapterPosition) }
        track_score.setOnClickListener { listener.onScoreClick(bindingAdapterPosition) }
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: TrackItem) {
        val track = item.track
        track_logo.setImageResource(item.service.getLogo())
        logo_container.setBackgroundColor(item.service.getLogoColor())

        track_set.visibleIf { track == null }
        track_title.visibleIf { track != null }

        track_details.visibleIf { track != null }
        if (track != null) {
            track_title.text = track.title
            track_chapters.text = "${track.last_chapter_read}/" +
                    if (track.total_chapters > 0) track.total_chapters else "-"
            track_status.text = item.service.getStatus(track.status)
            track_score.text = if (track.score == 0f) "-" else item.service.displayScore(track)
        }
    }
}
