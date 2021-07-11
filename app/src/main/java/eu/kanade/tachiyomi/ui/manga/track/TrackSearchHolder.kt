package eu.kanade.tachiyomi.ui.manga.track

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.clear
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.TrackSearchItemBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import java.util.Locale

class TrackSearchHolder(
    private val binding: TrackSearchItemBinding,
    private val adapter: TrackSearchAdapter
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(track: TrackSearch, position: Int) {
        binding.container.isChecked = position == adapter.selectedItemPosition
        binding.container.setOnClickListener {
            adapter.selectedItemPosition = position
            binding.container.isChecked = true
        }

        binding.trackSearchTitle.text = track.title
        binding.trackSearchCover.clear()
        if (track.cover_url.isNotEmpty()) {
            binding.trackSearchCover.loadAny(track.cover_url) {
                transformations(RoundedCornersTransformation(6.dpToPx.toFloat()))
            }
        }

        val hasStatus = track.publishing_status.isNotBlank()
        binding.trackSearchStatus.isVisible = hasStatus
        binding.trackSearchStatusResult.isVisible = hasStatus
        if (hasStatus) {
            binding.trackSearchStatusResult.text = track.publishing_status.lowercase().replaceFirstChar {
                it.titlecase(Locale.getDefault())
            }
        }

        val hasType = track.publishing_type.isNotBlank()
        binding.trackSearchType.isVisible = hasType
        binding.trackSearchTypeResult.isVisible = hasType
        if (hasType) {
            binding.trackSearchTypeResult.text = track.publishing_type.lowercase().replaceFirstChar {
                it.titlecase(Locale.getDefault())
            }
        }

        val hasStartDate = track.start_date.isNotBlank()
        binding.trackSearchStart.isVisible = hasStartDate
        binding.trackSearchStartResult.isVisible = hasStartDate
        if (hasStartDate) {
            binding.trackSearchStartResult.text = track.start_date
        }

        val hasSummary = track.summary.isNotBlank()
        binding.trackSearchSummary.isVisible = hasSummary
        if (hasSummary) {
            binding.trackSearchSummary.text = track.summary
        }
    }

    fun setUnchecked() {
        binding.container.isChecked = false
    }
}
