package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.TrackItemBinding
import eu.kanade.tachiyomi.util.view.popupMenu
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat

class TrackHolder(private val binding: TrackItemBinding, adapter: TrackAdapter) : RecyclerView.ViewHolder(binding.root) {

    private val preferences: PreferencesHelper by injectLazy()

    private val dateFormat: DateFormat by lazy {
        preferences.dateFormat()
    }

    private val listener = adapter.rowClickListener

    init {
        binding.trackSet.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        binding.trackTitle.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        binding.trackTitle.setOnLongClickListener {
            listener.onTitleLongClick(bindingAdapterPosition)
            true
        }
        binding.trackStatus.setOnClickListener { listener.onStatusClick(bindingAdapterPosition) }
        binding.trackChapters.setOnClickListener { listener.onChaptersClick(bindingAdapterPosition) }
        binding.trackScore.setOnClickListener { listener.onScoreClick(bindingAdapterPosition) }
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: TrackItem) {
        val track = item.track
        binding.trackLogo.setImageResource(item.service.getLogo())
        binding.logoContainer.setCardBackgroundColor(item.service.getLogoColor())

        binding.trackSet.isVisible = track == null
        binding.trackTitle.isVisible = track != null
        binding.more.isVisible = track != null

        binding.middleRow.isVisible = track != null
        binding.bottomDivider.isVisible = track != null
        binding.bottomRow.isVisible = track != null

        binding.card.isVisible = track != null

        if (track != null) {
            val ctx = binding.trackTitle.context
            binding.trackTitle.text = track.title
            binding.trackChapters.text = track.last_chapter_read.toString()
            if (track.total_chapters > 0) {
                binding.trackChapters.text = "${binding.trackChapters.text} / ${track.total_chapters}"
            }
            binding.trackStatus.text = item.service.getStatus(track.status)

            val supportsScoring = item.service.getScoreList().isNotEmpty()
            if (supportsScoring) {
                if (track.score != 0F) {
                    item.service.getScoreList()
                    binding.trackScore.text = item.service.displayScore(track)
                    binding.trackScore.alpha = SET_STATUS_TEXT_ALPHA
                } else {
                    binding.trackScore.text = ctx.getString(R.string.score)
                    binding.trackScore.alpha = UNSET_STATUS_TEXT_ALPHA
                }
            }
            binding.trackScore.isVisible = supportsScoring
            binding.vertDivider2.isVisible = supportsScoring

            val supportsReadingDates = item.service.supportsReadingDates
            if (supportsReadingDates) {
                if (track.started_reading_date != 0L) {
                    binding.trackStartDate.text = dateFormat.format(track.started_reading_date)
                    binding.trackStartDate.alpha = SET_STATUS_TEXT_ALPHA
                    binding.trackStartDate.setOnClickListener {
                        it.popupMenu(R.menu.track_item_date) {
                            when (itemId) {
                                R.id.action_edit -> listener.onStartDateEditClick(bindingAdapterPosition)
                                R.id.action_remove -> listener.onStartDateRemoveClick(bindingAdapterPosition)
                            }
                        }
                    }
                } else {
                    binding.trackStartDate.text = ctx.getString(R.string.track_started_reading_date)
                    binding.trackStartDate.alpha = UNSET_STATUS_TEXT_ALPHA
                    binding.trackStartDate.setOnClickListener {
                        listener.onStartDateEditClick(bindingAdapterPosition)
                    }
                }
                if (track.finished_reading_date != 0L) {
                    binding.trackFinishDate.text = dateFormat.format(track.finished_reading_date)
                    binding.trackFinishDate.alpha = SET_STATUS_TEXT_ALPHA
                    binding.trackFinishDate.setOnClickListener {
                        it.popupMenu(R.menu.track_item_date) {
                            when (itemId) {
                                R.id.action_edit -> listener.onFinishDateEditClick(bindingAdapterPosition)
                                R.id.action_remove -> listener.onFinishDateRemoveClick(bindingAdapterPosition)
                            }
                        }
                    }
                } else {
                    binding.trackFinishDate.text = ctx.getString(R.string.track_finished_reading_date)
                    binding.trackFinishDate.alpha = UNSET_STATUS_TEXT_ALPHA
                    binding.trackFinishDate.setOnClickListener {
                        listener.onFinishDateEditClick(bindingAdapterPosition)
                    }
                }
            }
            binding.bottomDivider.isVisible = supportsReadingDates
            binding.bottomRow.isVisible = supportsReadingDates

            binding.more.setOnClickListener {
                it.popupMenu(R.menu.track_item) {
                    when (itemId) {
                        R.id.action_open_in_browser -> {
                            listener.onOpenInBrowserClick(bindingAdapterPosition)
                        }
                        R.id.action_remove -> {
                            listener.onRemoveItemClick(bindingAdapterPosition)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val SET_STATUS_TEXT_ALPHA = 1F
        private const val UNSET_STATUS_TEXT_ALPHA = 0.5F
    }
}
