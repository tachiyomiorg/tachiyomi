package eu.kanade.tachiyomi.ui.manga.track

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.databinding.TrackItemBinding

class TrackAdapter(listener: OnClickListener) : RecyclerView.Adapter<TrackHolder>() {

    private lateinit var binding: TrackItemBinding

    var items = emptyList<TrackItem>()
        set(value) {
            if (field !== value) {
                field = value
                notifyDataSetChanged()
            }
        }

    val rowClickListener: OnClickListener = listener

    fun getItem(index: Int): TrackItem? {
        return items.getOrNull(index)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder {
        binding = TrackItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackHolder(binding, this)
    }

    override fun onBindViewHolder(holder: TrackHolder, position: Int) {
        holder.bind(items[position])
    }

    interface OnClickListener {
        fun onLogoClick(position: Int)
        fun onSetClick(position: Int)
        fun onTitleLongClick(position: Int)
        fun onStatusClick(position: Int)
        fun onChaptersClick(position: Int)
        fun onScoreClick(position: Int)
        fun onStartDateClick(position: Int)
        fun onFinishDateClick(position: Int)
    }
}
