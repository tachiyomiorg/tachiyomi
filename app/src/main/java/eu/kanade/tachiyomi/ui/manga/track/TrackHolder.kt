package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.TrackItemBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import uy.kohesive.injekt.injectLazy

class TrackHolder(private val binding: TrackItemBinding, adapter: TrackAdapter) : BaseViewHolder(binding.root) {

    private val preferences: PreferencesHelper by injectLazy()

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
        }
    }
}
