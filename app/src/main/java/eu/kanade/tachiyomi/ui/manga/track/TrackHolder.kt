package eu.kanade.tachiyomi.ui.manga.track

import android.support.v7.widget.RecyclerView
import android.view.View
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.item_track.view.*

class TrackHolder(private val view: View, private val fragment: TrackFragment)
: RecyclerView.ViewHolder(view) {
    
    private lateinit var item: TrackItem

    init {
        view.title_container.setOnClickListener { fragment.onTitleClick(item) }
        view.status_container.setOnClickListener { fragment.onStatusClick(item) }
        view.chapters_container.setOnClickListener { fragment.onChaptersClick(item) }
        view.score_container.setOnClickListener { fragment.onScoreClick(item) }
    }

    @Suppress("DEPRECATION")
    fun onSetValues(item: TrackItem) = with(view) {
        this@TrackHolder.item = item
        val sync = item.sync
        track_logo.setImageResource(item.service.getLogo())
        logo.setBackgroundColor(item.service.getLogoColor())
        if (sync != null) {
            track_title.setTextAppearance(context, R.style.TextAppearance_Regular_Body1_Secondary)
            track_title.setAllCaps(false)
            track_title.text = sync.title
            track_chapters.text = "${sync.last_chapter_read}/" +
                    if (sync.total_chapters > 0) sync.total_chapters else "-"
            track_status.text = item.service.getStatus(sync.status)
            track_score.text = if (sync.score == 0f) "-" else item.service.formatScore(sync)
        } else {
            track_title.setTextAppearance(context, R.style.TextAppearance_Medium_Button)
            track_title.setText(R.string.action_edit)
            track_chapters.text = ""
            track_score.text = ""
            track_status.text = ""
        }
    }
}