package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.inflate

/**
 * Item that contains search result information.
 *
 * @param source contains information about search result.
 */
class CatalogueSearchItem(val source: CatalogueSource, val results: List<CatalogueSearchCardItem>?)
    : AbstractFlexibleItem<CatalogueSearchHolder>() {

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
        holder.bind(this)
    }

    /**
     * Used to check if two items are equal.
     *
     * @return items are equal?
     */
    override fun equals(other: Any?): Boolean {
        if (other is CatalogueSearchItem) {
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