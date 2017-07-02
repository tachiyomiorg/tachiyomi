package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.inflate

/**
 * Item that contains search result information.
 *
 * @param searchResult contains information about search result.
 */
class CatalogueSearchItem(val searchResult: Pair<List<Manga>, CatalogueSource>) : AbstractFlexibleItem<CatalogueSearchHolder>() {

    /**
     * Set view.
     *
     * @return id of view
     */
    override fun getLayoutRes(): Int {
        return R.layout.catalogue_global_search_controller_card
    }

    /**
     * Create view holder (see [CatalogueSearchAdapter].
     *
     * @return holder of view.
     */
    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueSearchHolder {
        return CatalogueSearchHolder(parent.inflate(layoutRes), adapter as CatalogueSearchAdapter)
    }

    /**
     * Bind item to view.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CatalogueSearchHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(searchResult)
    }

    /**
     * Used to check if two items are equal.
     *
     * @return items are equal?
     */
    override fun equals(other: Any?): Boolean {
        if (other is CatalogueSearchItem) {
            return searchResult.second.id == other.searchResult.second.id
        }
        return false
    }

    /**
     * Return hash code of item.
     *
     * @return hashcode
     */
    override fun hashCode(): Int {
        return searchResult.second.id.toInt()
    }

}