package eu.kanade.tachiyomi.ui.manga.myanimelist

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.dialog_myanimelist_search_item.view.*
import java.util.*

class MyAnimeListSearchAdapter(context: Context) :
        ArrayAdapter<MangaSync>(context, R.layout.dialog_myanimelist_search_item, ArrayList<MangaSync>()) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var v = view
        // Get the data item for this position
        val sync = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        val holder: SearchViewHolder // view lookup cache stored in tag
        if (v == null) {
            v = parent.inflate(R.layout.dialog_myanimelist_search_item)
            holder = SearchViewHolder(v)
            v.tag = holder
        } else {
            holder = v.tag as SearchViewHolder
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

    class SearchViewHolder(private val view: View) {

        fun onSetValues(sync: MangaSync) {
            view.myanimelist_result_title.text = sync.title
        }
    }
}
