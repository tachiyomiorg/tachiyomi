package eu.kanade.tachiyomi.ui.manga.info

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.util.inflate
import java.util.*

/**
 * Adapter of [MangaSyncSearchDialogHolder].
 * Connection between Fragment and Holder
 * Holder updates should be called from here.
 *
 * @param context context of application
 * @constructor creates an instance of the adapter.
 */
class MangaSyncSearchDialogAdapter(context: Context) :
        ArrayAdapter<MangaSync>(context, R.layout.dialog_manga_sync_search_result, ArrayList<MangaSync>()) {

    /**
     * Get a View that displays the data at the specified position in the data set.
     * @param position The position of the item within the adapter's data set
     * @param convertView The old view to reuse, if possible.
     * @param parent he parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        // Get the data item for this position
        val mangaSync = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        val holder: MangaSyncSearchDialogHolder // view lookup cache stored in tag
        if (view == null) {
            view = parent.inflate(R.layout.dialog_manga_sync_search_result)
            holder = MangaSyncSearchDialogHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as MangaSyncSearchDialogHolder
        }
        holder.onSetValues(mangaSync)
        return view
    }

    /**
     * This method will set the items of the [MangaSyncSearchDialogHolder]
     * @param mangaSync list containing items of type [MangaSync]
     */
    fun setItems(mangaSync: List<MangaSync>) {
        setNotifyOnChange(false)
        clear()
        addAll(mangaSync)
        notifyDataSetChanged()
    }
}