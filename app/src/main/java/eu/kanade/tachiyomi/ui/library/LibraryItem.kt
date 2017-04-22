package eu.kanade.tachiyomi.ui.library

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFilterable
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.item_catalogue_grid.view.*

class LibraryItem(val manga: Manga) : AbstractFlexibleItem<LibraryHolder>(), IFilterable {

    override fun getLayoutRes(): Int {
        return R.layout.item_catalogue_grid
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>,
                                  inflater: LayoutInflater,
                                  parent: ViewGroup): LibraryHolder {

        return if (parent is AutofitRecyclerView) {
            val view = parent.inflate(R.layout.item_catalogue_grid).apply {
                val coverHeight = parent.itemWidth / 3 * 4
                card.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight)
                gradient.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT, coverHeight / 2, Gravity.BOTTOM)
            }
            LibraryGridHolder(view, adapter)
        } else {
            val view = parent.inflate(R.layout.item_catalogue_list)
            LibraryListHolder(view, adapter)
        }
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>,
                                holder: LibraryHolder,
                                position: Int,
                                payloads: List<Any?>?) {

        holder.onSetValues(manga)
    }

    /**
     * Filters a manga depending on a query.
     *
     * @param constraint the query to apply.
     * @return true if the manga should be included, false otherwise.
     */
    override fun filter(constraint: String): Boolean {
        return manga.title.contains(constraint, true) ||
                (manga.author?.contains(constraint, true) ?: false)
    }

    override fun equals(other: Any?): Boolean {
        if (other is LibraryItem) {
            return manga.id == other.manga.id
        }
        return false
    }

    override fun hashCode(): Int {
        return manga.id!!.hashCode()
    }
}