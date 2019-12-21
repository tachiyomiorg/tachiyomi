package eu.kanade.tachiyomi.ui.manga.track

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.track_search_item.view.*
import java.util.*

class TrackSearchAdapter(context: Context)
    : ArrayAdapter<TrackSearch>(context, R.layout.track_search_item, ArrayList<TrackSearch>()) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var v = view
        // Get the data item for this position
        val track = getItem(position)
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

        fun onSetValues(track: TrackSearch) {
            view.track_search_title.text = track.title
            view.track_search_summary.text = track.summary
            GlideApp.with(view.context).clear(view.track_search_cover)
            if (!track.cover_url.isNullOrEmpty()) {
                GlideApp.with(view.context)
                        .load(track.cover_url)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .into(view.track_search_cover)
            }

            if (track.publishing_status.isNullOrBlank()) {
                view.track_search_status.gone()
                view.track_search_status_result.gone()
            } else {
                view.track_search_status_result.text = track.publishing_status.capitalize()
            }

            if (track.publishing_type.isNullOrBlank()) {
                view.track_search_type.gone()
                view.track_search_type_result.gone()
            } else {
                view.track_search_type_result.text = track.publishing_type.capitalize()
            }

            if (track.start_date.isNullOrBlank()) {
                view.track_search_start.gone()
                view.track_search_start_result.gone()
            } else {
                view.track_search_start_result.text = track.start_date
            }
        }
    }
}