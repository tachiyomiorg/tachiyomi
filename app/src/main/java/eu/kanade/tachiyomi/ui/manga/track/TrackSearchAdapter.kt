package eu.kanade.tachiyomi.ui.manga.track

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.item_track_search.view.*
import java.util.*

class TrackSearchAdapter(context: Context)
: ArrayAdapter<MangaSync>(context, R.layout.item_track_search, ArrayList<MangaSync>()) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var v = view
        // Get the data item for this position
        val sync = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        val holder: TrackSearchHolder // view lookup cache stored in tag
        if (v == null) {
            v = parent.inflate(R.layout.item_track_search)
            holder = TrackSearchHolder(v)
            v.tag = holder
        } else {
            holder = v.tag as TrackSearchHolder
        }
        holder.onSetValues(sync)
        return v
    }

    fun setItems(syncs: List<MangaSync>) {
        setNotifyOnChange(false)
        clear()
        addAll(syncs)
        notifyDataSetChanged()
    }

    class TrackSearchHolder(private val view: View) {

        fun onSetValues(sync: MangaSync) {
            view.track_search_title.text = sync.title
        }
    }

}