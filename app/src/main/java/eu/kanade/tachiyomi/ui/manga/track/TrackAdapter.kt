package eu.kanade.tachiyomi.ui.manga.track

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.inflate

class TrackAdapter(val fragment: TrackFragment) : RecyclerView.Adapter<TrackHolder>() {

    var items = emptyList<TrackItem>()
        set(value) {
            if (field !== value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var onClickListener: (TrackItem) -> Unit = {}

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder {
        val view = parent.inflate(R.layout.item_track)
        return TrackHolder(view, fragment)
    }

    override fun onBindViewHolder(holder: TrackHolder, position: Int) {
        holder.onSetValues(items[position])
    }

}