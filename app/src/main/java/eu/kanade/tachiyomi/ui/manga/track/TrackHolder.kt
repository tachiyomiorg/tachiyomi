package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.TrackItemBinding
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat

class TrackHolder(private val binding: TrackItemBinding, private val adapter: TrackAdapter) : RecyclerView.ViewHolder(binding.root) {

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
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: TrackItem) {
        val track = item.track
        binding.trackLogo.setImageResource(item.service.getLogo())
        binding.logoContainer.setCardBackgroundColor(item.service.getLogoColor())

        binding.trackSet.isVisible = track == null
        binding.trackTitle.isVisible = track != null

        binding.middleRow.isVisible = track != null
        binding.bottomDivider.isVisible = track != null
        binding.bottomRow.isVisible = track != null

        binding.itemDivider.isVisible = adapter.items.indexOf(item) != 0
        binding.card.isVisible = track != null

        if (track != null) {
            val ctx = binding.trackTitle.context
            binding.trackTitle.text = track.title
            binding.trackChapters.text = "${track.last_chapter_read} / " +
                if (track.total_chapters > 0) track.total_chapters else "-"
            binding.trackStatus.text = item.service.getStatus(track.status)

            val supportsScoring = item.service.getScoreList().isNotEmpty()
            if (supportsScoring) {
                if (track.score != 0F) {
                    item.service.getScoreList()
                    binding.trackScore.text = item.service.displayScore(track)
                    binding.trackScore.alpha = 1F
                } else {
                    binding.trackScore.text = ctx.getString(R.string.score)
                    binding.trackScore.alpha = 0.5F
                }
            }
            binding.trackScore.isVisible = supportsScoring
            binding.vertDivider2.isVisible = supportsScoring

            val supportsReadingDates = item.service.supportsReadingDates
            if (supportsReadingDates) {
                if (track.started_reading_date != 0L) {
                    binding.trackStartDate.text = dateFormat.format(track.started_reading_date)
                    binding.trackStartDate.alpha = 1F
                } else {
                    binding.trackStartDate.text = ctx.getString(R.string.track_started_reading_date)
                    binding.trackStartDate.alpha = 0.5F
                }
                if (track.finished_reading_date != 0L) {
                    binding.trackFinishDate.text = dateFormat.format(track.finished_reading_date)
                    binding.trackFinishDate.alpha = 1F
                } else {
                    binding.trackFinishDate.text = ctx.getString(R.string.track_finished_reading_date)
                    binding.trackFinishDate.alpha = 0.5F
                }
            }
            binding.bottomDivider.isVisible = supportsReadingDates
            binding.bottomRow.isVisible = supportsReadingDates
        }
    }
}
