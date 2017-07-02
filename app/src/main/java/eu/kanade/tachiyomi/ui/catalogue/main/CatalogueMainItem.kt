package eu.kanade.tachiyomi.ui.catalogue.main

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.inflate

/**
 * Item that contains catalogue card information.
 *
 * @param sourcePair contains language information and an instance of [CatalogueSource] containing source information.
 */
class CatalogueMainItem(val sourcePair: Pair<String, List<CatalogueSource>>) : AbstractFlexibleItem<CatalogueMainHolder>() {

    /**
     * Set view.
     *
     * @return id of view
     */
    override fun getLayoutRes(): Int {
        return R.layout.catalogue_main_controller_card
    }

    /**
     * Create view holder (see [CatalogueMainHolder].
     *
     * @return holder of view.
     */
    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueMainHolder {
        return CatalogueMainHolder(parent.inflate(layoutRes), adapter as CatalogueMainAdapter)
    }

    /**
     * Bind item to view.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CatalogueMainHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(sourcePair)
    }

    /**
     * Used to check if two items are equal.
     *
     * @return items are equal?
     */
    override fun equals(other: Any?): Boolean {
        if (other is CatalogueMainItem) {
            return sourcePair.first == other.sourcePair.first
        }
        return false
    }

    /**
     * Return hash code of item.
     *
     * @return hashcode
     */
    override fun hashCode(): Int {
        return sourcePair.first.hashCode()
    }

}