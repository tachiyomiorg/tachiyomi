package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.View
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.track_item.view.*

class TrackHolder(view: View, adapter: TrackAdapter) : RecyclerView.ViewHolder(view) {
    
    init {
        val listener = adapter.rowClickListener
        view.title_container.setOnClickListener { listener.onTitleClick(adapterPosition) }
        view.status_container.setOnClickListener { listener.onStatusClick(adapterPosition) }
        view.chapters_container.setOnClickListener { listener.onChaptersClick(adapterPosition) }
        view.score_container.setOnClickListener { listener.onScoreClick(adapterPosition) }
    }

    @SuppressLint("SetTextI18n")
    @Suppress("DEPRECATION")
    fun bind(item: TrackItem) = with(itemView) {
        val track = item.track
        track_logo.setImageResource(item.service.getLogo())
        logo.setBackgroundColor(item.service.getLogoColor())
        if (track != null) {
            track_title.setTextAppearance(context, R.style.TextAppearance_Regular_Body1_Secondary)
            track_title.setAllCaps(false)
            track_title.text = track.title
            track_chapters.text = "${track.last_chapter_read}/" +
                    if (track.total_chapters > 0) track.total_chapters else "-"
            track_status.text = item.service.getStatus(track.status)
            track_score.text = if (track.score == 0f) "-" else item.service.displayScore(track)
        } else {
            track_title.setTextAppearance(context, R.style.TextAppearance_Medium_Button)
            track_title.setText(R.string.action_edit)
            track_chapters.text = ""
            track_score.text = ""
            track_status.text = ""
        }
    }
}
