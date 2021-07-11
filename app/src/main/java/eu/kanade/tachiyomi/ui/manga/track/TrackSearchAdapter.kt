package eu.kanade.tachiyomi.ui.manga.track

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.TrackSearchItemBinding
import eu.kanade.tachiyomi.util.view.applyElevationOverlay

class TrackSearchAdapter(
    private val currentTrackUrl: String?,
    private val onSelectionChanged: (TrackSearch?) -> Unit
) : RecyclerView.Adapter<TrackSearchHolder>() {
    var selectedItemPosition = -1
        set(value) {
            if (field != value) {
                val previousPosition = field
                field = value
                // Just notify the now-unselected item
                notifyItemChanged(previousPosition, UncheckPayload)
                onSelectionChanged(items.getOrNull(value))
            }
        }

    var items = emptyList<TrackSearch>()
        set(value) {
            if (field != value) {
                field = value
                selectedItemPosition = value.indexOfFirst { it.tracking_url == currentTrackUrl }
                notifyDataSetChanged()
            }
        }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackSearchHolder {
        val binding = TrackSearchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.container.applyElevationOverlay()
        return TrackSearchHolder(binding, this)
    }

    override fun onBindViewHolder(holder: TrackSearchHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun onBindViewHolder(holder: TrackSearchHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.getOrNull(0) == UncheckPayload) {
            holder.setUnchecked()
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    companion object {
        private object UncheckPayload
    }
}
