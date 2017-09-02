package eu.kanade.tachiyomi.ui.catalogue.main

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource

/**
 * Item that contains source information.
 *
 * @param source instance of [CatalogueSource] containing source information.
 */
class SourceItem(val source: CatalogueSource, header: LangItem, val index: Int, val count: Int) :
        AbstractSectionableItem<SourceHolder, LangItem>(header) {

    /**
     * Set view.
     *
     * @return id of view
     */
    override fun getLayoutRes(): Int {
        return R.layout.catalogue_main_controller_card_item
    }

    /**
     * Create view holder (see [SourceHolder]).
     *
     * @return holder of view.
     */
    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): SourceHolder {

        val view = inflater.inflate(layoutRes, parent, false)
        return SourceHolder(view, adapter as CatalogueMainAdapter)
    }

    /**
     * Bind item to holder.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: SourceHolder,
                                position: Int, payloads: List<Any?>?) {

        holder.bind(this)
    }

    /**
     * Used to check if two items are equal.
     *
     * @return items are equal?
     */
    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is SourceItem -> source.id == other.source.id
            else -> false
        }
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