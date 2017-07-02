package eu.kanade.tachiyomi.ui.catalogue.main.card

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.inflate

/**
 * Item that contains source information.
 *
 * @param source instance of [CatalogueSource] containing source information.
 */
class CatalogueMainCardItem(val source: CatalogueSource) : AbstractFlexibleItem<CatalogueMainCardHolder>() {

    /**
     * Set view.
     *
     * @return id of view
     */
    override fun getLayoutRes(): Int {
        return R.layout.catalogue_main_controller_card_item
    }

    /**
     * Create view holder (see [CatalogueMainCardHolder].
     *
     * @return holder of view.
     */
    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueMainCardHolder {
        return CatalogueMainCardHolder(parent.inflate(layoutRes), adapter as CatalogueMainCardAdapter)
    }

    /**
     * Bind item to view.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CatalogueMainCardHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(source)
    }

    /**
     * Used to check if two items are equal.
     *
     * @return items are equal?
     */
    override fun equals(other: Any?): Boolean {
        if (other is CatalogueMainCardItem) {
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