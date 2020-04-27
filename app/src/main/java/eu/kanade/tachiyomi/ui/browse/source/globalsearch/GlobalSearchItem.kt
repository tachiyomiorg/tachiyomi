package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource

/**
 * Item that contains search result information.
 *
 * @param source the source for the search results.
 * @param results the search results.
 * @param highlighted whether this search item should be highlighted/marked in the catalogue search view.
 */
class GlobalSearchItem(val source: CatalogueSource, val results: List<GlobalSearchCardItem>?, val highlighted: Boolean = false) :
    AbstractFlexibleItem<GlobalSearchHolder>() {

    /**
     * Set view.
     *
     * @return id of view
     */
    override fun getLayoutRes(): Int {
        return R.layout.global_search_controller_card
    }

    /**
     * Create view holder (see [GlobalSearchAdapter].
     *
     * @return holder of view.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): GlobalSearchHolder {
        return GlobalSearchHolder(view, adapter as GlobalSearchAdapter)
    }

    /**
     * Bind item to view.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: GlobalSearchHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.bind(this)
    }

    /**
     * Used to check if two items are equal.
     *
     * @return items are equal?
     */
    override fun equals(other: Any?): Boolean {
        if (other is GlobalSearchItem) {
            return source.id == other.source.id
        }
        return false
    }

    /**
     * Return hash code of item.
     *
     * @return hashcode
     */
    override fun hashCode(): Int {
        return source.id.toInt()
    }
}
