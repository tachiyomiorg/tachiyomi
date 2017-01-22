package eu.kanade.tachiyomi.ui.library

import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import eu.davidea.flexibleadapter4.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.item_catalogue_grid.view.*
import java.util.*

/**
 * Adapter storing a list of manga in a certain category.
 *
 * @param fragment the fragment containing this adapter.
 */
class LibraryCategoryAdapter(val fragment: LibraryCategoryView) :
        FlexibleAdapter<LibraryHolder, Manga>() {

    /**
     * The list of manga in this category.
     */
    private var mangas: List<Manga> = emptyList()

    init {
        setHasStableIds(true)
    }

    /**
     * Sets a list of manga in the adapter.
     *
     * @param list the list to set.
     */
    fun setItems(list: List<Manga>) {
        mItems = list

        // A copy of manga always unfiltered.
        mangas = ArrayList(list)
        updateDataSet(null)
    }

    /**
     * Returns the identifier for a manga.
     *
     * @param position the position in the adapter.
     * @return an identifier for the item.
     */
    override fun getItemId(position: Int): Long {
        return mItems[position].id!!
    }

    /**
     * Filters the list of manga applying [filterObject] for each element.
     *
     * @param param the filter. Not used.
     */
    override fun updateDataSet(param: String?) {
        filterItems(mangas)
        notifyDataSetChanged()
    }

    /**
     * Filters a manga depending on a query.
     *
     * @param manga the manga to filter.
     * @param query the query to apply.
     * @return true if the manga should be included, false otherwise.
     */
    override fun filterObject(manga: Manga, query: String): Boolean = with(manga) {
        title.toLowerCase().contains(query) ||
                author != null && author!!.toLowerCase().contains(query)
    }

    /**
     * Creates a new view holder.
     *
     * @param parent the parent view.
     * @param viewType the type of the holder.
     * @return a new view holder for a manga.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryHolder {
        // Depending on preferences, display a list or display a grid
        if (parent is AutofitRecyclerView) {
            val view = parent.inflate(R.layout.item_catalogue_grid).apply {
                val coverHeight = parent.itemWidth / 3 * 4
                card.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight)
                gradient.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight / 2, Gravity.BOTTOM)
            }
            return LibraryGridHolder(view, this, fragment)
        } else {
            val view = parent.inflate(R.layout.item_catalogue_list)
            return LibraryListHolder(view, this, fragment)
        }
    }

    /**
     * Binds a holder with a new position.
     *
     * @param holder the holder to bind.
     * @param position the position to bind.
     */
    override fun onBindViewHolder(holder: LibraryHolder, position: Int) {
        val manga = getItem(position)

        holder.onSetValues(manga)
        // When user scrolls this bind the correct selection status
        holder.itemView.isActivated = isSelected(position)
    }

    /**
     * Returns the position in the adapter for the given manga.
     *
     * @param manga the manga to find.
     */
    fun indexOf(manga: Manga): Int {
        return mangas.orEmpty().indexOfFirst { it.id == manga.id }
    }

}
