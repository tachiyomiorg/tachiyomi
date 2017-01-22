package eu.kanade.tachiyomi.ui.download

import android.content.Context
import android.view.ViewGroup
import eu.davidea.flexibleadapter4.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.util.inflate

/**
 * Adapter storing a list of downloads.
 *
 * @param context the context of the fragment containing this adapter.
 */
class DownloadAdapter(private val context: Context) : FlexibleAdapter<DownloadHolder, Download>() {

    init {
        setHasStableIds(true)
    }

    /**
     * Sets a list of downloads in the adapter.
     *
     * @param downloads the list to set.
     */
    fun setItems(downloads: List<Download>) {
        mItems = downloads
        notifyDataSetChanged()
    }

    /**
     * Returns the identifier for a download.
     *
     * @param position the position in the adapter.
     * @return an identifier for the item.
     */
    override fun getItemId(position: Int): Long {
        return getItem(position).chapter.id!!
    }

    /**
     * Creates a new view holder.
     *
     * @param parent the parent view.
     * @param viewType the type of the holder.
     * @return a new view holder for a manga.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadHolder {
        val view = parent.inflate(R.layout.item_download)
        return DownloadHolder(view)
    }

    /**
     * Binds a holder with a new position.
     *
     * @param holder the holder to bind.
     * @param position the position to bind.
     */
    override fun onBindViewHolder(holder: DownloadHolder, position: Int) {
        val download = getItem(position)
        holder.onSetValues(download)
    }

    /**
     * Used to filter the list. Not used.
     */
    override fun updateDataSet(param: String) {
    }

}
