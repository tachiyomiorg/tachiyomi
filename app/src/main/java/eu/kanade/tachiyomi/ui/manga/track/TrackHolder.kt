package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys.dateFormat
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.util.view.visibleIf
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.android.synthetic.main.track_item.chapters_container
import kotlinx.android.synthetic.main.track_item.finish_date_container
import kotlinx.android.synthetic.main.track_item.logo_container
import kotlinx.android.synthetic.main.track_item.score_container
import kotlinx.android.synthetic.main.track_item.start_date_container
import kotlinx.android.synthetic.main.track_item.status_container
import kotlinx.android.synthetic.main.track_item.title_container
import kotlinx.android.synthetic.main.track_item.track_chapters
import kotlinx.android.synthetic.main.track_item.track_details
import kotlinx.android.synthetic.main.track_item.track_finish_date
import kotlinx.android.synthetic.main.track_item.track_logo
import kotlinx.android.synthetic.main.track_item.track_score
import kotlinx.android.synthetic.main.track_item.track_set
import kotlinx.android.synthetic.main.track_item.track_start_date
import kotlinx.android.synthetic.main.track_item.track_status
import kotlinx.android.synthetic.main.track_item.track_title
import java.text.DateFormat

class TrackHolder(view: View, adapter: TrackAdapter) : BaseViewHolder(view) {

    private val context = view.context

    init {
        val listener = adapter.rowClickListener

        logo_container.setOnClickListener { listener.onLogoClick(adapterPosition) }
        title_container.setOnClickListener { listener.onTitleClick(adapterPosition) }
        status_container.setOnClickListener { listener.onStatusClick(adapterPosition) }
        chapters_container.setOnClickListener { listener.onChaptersClick(adapterPosition) }
        score_container.setOnClickListener { listener.onScoreClick(adapterPosition) }
        start_date_container.setOnClickListener { listener.onStartDateClick(adapterPosition) }
        finish_date_container.setOnClickListener { listener.onFinishDateClick(adapterPosition) }

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

            if (item.service.supports_reading_dates) {
                start_date_container.visible()
                finish_date_container.visible() // Keep hidden if status is reading?

                val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

                track_start_date.text = track.started_reading_date?.let { dateFormat.format(it.time) } ?: "-"
                track_finish_date.text = track.finished_reading_date?.let { dateFormat.format(it.time) } ?: "-"
            }
        }
    }
}
