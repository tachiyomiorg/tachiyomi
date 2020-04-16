package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import android.view.View
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visibleIf
import java.text.DateFormat
import kotlinx.android.synthetic.main.track_item.bottom_divider
import kotlinx.android.synthetic.main.track_item.logo_container
import kotlinx.android.synthetic.main.track_item.track_chapters
import kotlinx.android.synthetic.main.track_item.track_details
import kotlinx.android.synthetic.main.track_item.track_finish_date
import kotlinx.android.synthetic.main.track_item.track_logo
import kotlinx.android.synthetic.main.track_item.track_score
import kotlinx.android.synthetic.main.track_item.track_set
import kotlinx.android.synthetic.main.track_item.track_start_date
import kotlinx.android.synthetic.main.track_item.track_status
import kotlinx.android.synthetic.main.track_item.track_title
import kotlinx.android.synthetic.main.track_item.vert_divider_3
import uy.kohesive.injekt.injectLazy

class TrackHolder(view: View, adapter: TrackAdapter) : BaseViewHolder(view) {

    private val preferences: PreferencesHelper by injectLazy()

    private val dateFormat: DateFormat by lazy {
        preferences.dateFormat().getOrDefault()
    }

    init {
        val listener = adapter.rowClickListener

        logo_container.setOnClickListener { listener.onLogoClick(adapterPosition) }
        track_set.setOnClickListener { listener.onSetClick(adapterPosition) }
        track_title.setOnClickListener { listener.onSetClick(adapterPosition) }
        track_status.setOnClickListener { listener.onStatusClick(adapterPosition) }
        track_chapters.setOnClickListener { listener.onChaptersClick(adapterPosition) }
        track_score.setOnClickListener { listener.onScoreClick(adapterPosition) }
        track_start_date.setOnClickListener { listener.onStartDateClick(adapterPosition) }
        track_finish_date.setOnClickListener { listener.onFinishDateClick(adapterPosition) }
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

            if (item.service.supportsReadingDates) {
                track_start_date.text =
                        if (track.started_reading_date != 0L) dateFormat.format(track.started_reading_date) else "-"
                track_finish_date.text =
                        if (track.finished_reading_date != 0L) dateFormat.format(track.finished_reading_date) else "-"
            } else {
                bottom_divider.gone()
                vert_divider_3.gone()
                track_start_date.gone()
                track_finish_date.gone()
            }
        }
    }
}
