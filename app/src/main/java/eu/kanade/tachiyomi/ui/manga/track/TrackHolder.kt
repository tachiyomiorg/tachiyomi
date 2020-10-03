package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.TrackItemBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import eu.kanade.tachiyomi.util.view.popupMenu
import java.text.DateFormat
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat

class TrackHolder(private val binding: TrackItemBinding, adapter: TrackAdapter) : BaseViewHolder(binding.root) {

    private val preferences: PreferencesHelper by injectLazy()

    private val dateFormat: DateFormat by lazy {
        preferences.dateFormat()
    }

    private val listener = adapter.rowClickListener

    init {
        binding.logoContainer.setOnClickListener { listener.onLogoClick(bindingAdapterPosition) }
        binding.trackSet.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        binding.trackTitle.setOnClickListener { listener.onSetClick(bindingAdapterPosition) }
        binding.trackTitle.setOnLongClickListener {
            listener.onTitleLongClick(bindingAdapterPosition)
            true
        }
        binding.trackOpenMenu.setOnClickListener { it.post { showPopupMenu(it, adapter.getItem(position)?.track) } }
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
        binding.logoContainer.setBackgroundColor(item.service.getLogoColor())

        binding.trackSet.isVisible = track == null
        binding.trackTitle.isVisible = track != null
        binding.trackMenu.isVisible = track != null

        binding.trackDetails.isVisible = track != null
        if (track != null) {
            binding.trackTitle.text = track.title
            binding.trackChapters.text = "${track.last_chapter_read}/" +
                if (track.total_chapters > 0) track.total_chapters else "-"
            binding.trackStatus.text = item.service.getStatus(track.status)
            binding.trackScore.text = if (track.score == 0f) "-" else item.service.displayScore(track)

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

    private fun showPopupMenu(view: View, track: Track?) {
        val latestTrackedChapter = track?.last_chapter_read
        view.popupMenu(
            R.menu.track,
            {
                findItem(R.id.track_get_chapters).isVisible = latestTrackedChapter != 0
                findItem(R.id.track_remove)
            },
            {
                listener.onMenuItemClick(bindingAdapterPosition, this)
                true
            }
        )
    }
}