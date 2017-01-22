package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.inflate

/**
 * Adapter of pages for a RecyclerView.
 *
 * @param fragment the fragment containing this adapter.
 */
class WebtoonAdapter(val fragment: WebtoonReader) : RecyclerView.Adapter<WebtoonHolder>() {

    /**
     * Pages stored in the adapter.
     */
    var pages: List<Page>? = null

    /**
     * Touch listener for images in holders.
     */
    val touchListener = View.OnTouchListener { v, ev -> fragment.imageGestureDetector.onTouchEvent(ev) }

    /**
     * Returns the number of pages.
     *
     * @return the number of pages or 0 if the list is null.
     */
    override fun getItemCount(): Int {
        return pages?.size ?: 0
    }

    /**
     * Returns a page given the position.
     *
     * @param position the position of the page.
     * @return the page.
     */
    fun getItem(position: Int): Page {
        return pages!![position]
    }

    /**
     * Creates a new view holder.
     *
     * @param parent the parent view.
     * @param viewType the type of the holder.
     * @return a new view holder for a manga.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebtoonHolder {
        val v = parent.inflate(R.layout.item_webtoon_reader)
        return WebtoonHolder(v, this)
    }

    /**
     * Binds a holder with a new position.
     *
     * @param holder the holder to bind.
     * @param position the position to bind.
     */
    override fun onBindViewHolder(holder: WebtoonHolder, position: Int) {
        val page = getItem(position)
        holder.onSetValues(page)
    }

    /**
     * Recycles the view holder.
     *
     * @param holder the holder to recycle.
     */
    override fun onViewRecycled(holder: WebtoonHolder) {
        holder.onRecycle()
    }

}
