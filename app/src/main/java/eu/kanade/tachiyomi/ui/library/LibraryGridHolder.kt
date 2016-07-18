package eu.kanade.tachiyomi.ui.library

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import kotlinx.android.synthetic.main.item_catalogue_grid.view.*

/**
 * Class used to hold the displayed data of a manga in the library, like the cover or the title.
 * All the elements from the layout file "item_catalogue_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new library holder.
 */
class LibraryGridHolder(private val view: View,
                        private val adapter: LibraryCategoryAdapter,
                        listener: FlexibleViewHolder.OnListItemClickListener)
: LibraryHolder(view, adapter, listener) {

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    override fun onSetValues(manga: Manga) {
        // Update the title of the manga.
        view.title.text = manga.title

        // Update the unread count and its visibility.
        with(view.unread_text) {
            visibility = if (manga.unread > 0) View.VISIBLE else View.GONE
            text = manga.unread.toString()
        }

        // Update the cover.
        Glide.clear(view.thumbnail)
        Glide.with(view.context)
                .load(manga)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .centerCrop()
                .into(view.thumbnail)
    }

}
