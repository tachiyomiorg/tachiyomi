package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.TrackItemBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import java.text.DateFormat
import uy.kohesive.injekt.injectLazy

class TrackHolder(private val binding: TrackItemBinding, adapter: TrackAdapter) : BaseViewHolder(binding.root) {

    private val preferences: PreferencesHelper by injectLazy()

    private val dateFormat: DateFormat by lazy {
        preferences.dateFormat()
    }

    init {
        val listener = adapter.rowClickListener

        binding.logoContainer.setOnClickListener { listener.onLogoClick(bindingAdapterPosition) }
        binding.trackSet.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        binding.trackTitle.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        binding.trackTitle.setOnLongClickListener {
            listener.onTitleLongClick(bindingAdapterPosition)
            true
        }
        binding.trackStatus.setOnClickListener { listener.onStatusClick(bindingAdapterPosition) }
        binding.trackChapters.setOnClickListener { listener.onChaptersClick(bindingAdapterPosition) }
        binding.trackScore.setOnClickListener { listener.onScoreClick(bindingAdapterPosition) }
        binding.trackStartDate.setOnClickListener { listener.onStartDateClick(bindingAdapterPosition) }
        binding.trackFinishDate.setOnClickListener { listener.onFinishDateClick(bindingAdapterPosition) }
        binding.getTrackChapters.setOnClickListener { listener.onGetChaptersClick(bindingAdapterPosition) }
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: TrackItem) {
        val track = item.track
        binding.trackLogo.setImageResource(item.service.getLogo())
        binding.logoContainer.setBackgroundColor(item.service.getLogoColor())

        binding.trackSet.isVisible = track == null
        binding.trackTitle.isVisible = track != null

        binding.trackDetails.isVisible = track != null
        if (track != null) {
            binding.trackTitle.text = track.title
            binding.trackChapters.text = "${track.last_chapter_read}/" +
                if (track.total_chapters > 0) track.total_chapters else "-"
            binding.trackStatus.text = item.service.getStatus(track.status)
            binding.trackScore.text = if (track.score == 0f) "-" else item.service.displayScore(track)
            binding.getTrackChapters.text = "Sync read chapters with Tachiyomi"

            if (item.service.supportsReadingDates) {
                binding.trackStartDate.text =
                    if (track.started_reading_date != 0L) dateFormat.format(track.started_reading_date) else "-"
                binding.trackFinishDate.text =
                    if (track.finished_reading_date != 0L) dateFormat.format(track.finished_reading_date) else "-"
            } else {
                binding.bottomDivider.isVisible = false
                binding.vertDivider3.isVisible = false
                binding.trackStartDate.isVisible = false
                binding.trackFinishDate.isVisible = false
            }
        }
    }
}
