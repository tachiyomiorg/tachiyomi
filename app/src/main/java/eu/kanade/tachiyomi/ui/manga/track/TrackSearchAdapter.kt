package eu.kanade.tachiyomi.ui.manga.track

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.TrackSearchItemBinding
import eu.kanade.tachiyomi.util.view.inflate

class TrackSearchAdapter(context: Context) :
    ArrayAdapter<TrackSearch>(context, R.layout.track_search_item, mutableListOf<TrackSearch>()) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var v = view
        // Get the data item for this position
        val track = getItem(position)!!
        // Check if an existing view is being reused, otherwise inflate the view
        val holder: TrackSearchHolder // view lookup cache stored in tag
        if (v == null) {
            v = parent.inflate(R.layout.track_search_item)
            holder = TrackSearchHolder(v)
            v.tag = holder
        } else {
            holder = v.tag as TrackSearchHolder
        }
        holder.onSetValues(track)
        return v
    }

    fun setItems(syncs: List<TrackSearch>) {
        setNotifyOnChange(false)
        clear()
        addAll(syncs)
        notifyDataSetChanged()
    }

    class TrackSearchHolder(private val view: View) {

        private val binding = TrackSearchItemBinding.bind(view)

        fun onSetValues(track: TrackSearch) {
            binding.trackSearchTitle.text = track.title
            binding.trackSearchSummary.text = track.summary
            GlideApp.with(view.context).clear(binding.trackSearchCover)
            if (track.cover_url.isNotEmpty()) {
                GlideApp.with(view.context)
                    .load(track.cover_url)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
                    .into(binding.trackSearchCover)
            }

            if (track.publishing_status.isBlank()) {
                binding.trackSearchStatus.isVisible = false
                binding.trackSearchStatusResult.isVisible = false
            } else {
                binding.trackSearchStatusResult.text = track.publishing_status.capitalize()
            }

            if (track.publishing_type.isBlank()) {
                binding.trackSearchType.isVisible = false
                binding.trackSearchTypeResult.isVisible = false
            } else {
                binding.trackSearchTypeResult.text = track.publishing_type.capitalize()
            }

            if (track.start_date.isBlank()) {
                binding.trackSearchStart.isVisible = false
                binding.trackSearchStartResult.isVisible = false
            } else {
                binding.trackSearchStartResult.text = track.start_date
            }
        }
    }
}
