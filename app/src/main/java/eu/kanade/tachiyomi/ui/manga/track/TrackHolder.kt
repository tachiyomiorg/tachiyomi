package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import eu.kanade.tachiyomi.util.visibleIf
import kotlinx.android.synthetic.main.track_item.*

class TrackHolder(view: View, adapter: TrackAdapter) : BaseViewHolder(view) {

    init {
        val listener = adapter.rowClickListener

        logo_container.setOnClickListener { listener.onLogoClick(adapterPosition) }
        title_container.setOnClickListener { listener.onTitleClick(adapterPosition) }
        status_container.setOnClickListener { listener.onStatusClick(adapterPosition) }
        chapters_container.setOnClickListener { listener.onChaptersClick(adapterPosition) }
        score_container.setOnClickListener { listener.onScoreClick(adapterPosition) }

        track_set.setOnClickListener { listener.onTitleClick(adapterPosition) }
    }

    @SuppressLint("SetTextI18n")
    @Suppress("DEPRECATION")
    fun bind(item: TrackItem) {
        val track = item.track
        track_logo.setImageResource(item.service.getLogo())
        logo_container.setBackgroundColor(item.service.getLogoColor())

        track_details.visibleIf { track != null }
        track_set.visibleIf { track == null }

        if (track != null) {
            track_title.setTextAppearance(itemView.context, R.style.TextAppearance_Regular_Body1_Secondary)
            track_title.isAllCaps = false
            track_title.text = track.title
            track_chapters.text = "${track.last_chapter_read}/" +
                    if (track.total_chapters > 0) track.total_chapters else "-"
            track_status.text = item.service.getStatus(track.status)
            track_score.text = if (track.score == 0f) "-" else item.service.displayScore(track)
        }
    }
}
