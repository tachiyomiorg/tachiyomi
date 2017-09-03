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
 * @param source Instance of [CatalogueSource] containing source information.
 * @param header The header for this item.
 */
data class SourceItem(val source: CatalogueSource, val header: LangItem? = null) :
        AbstractSectionableItem<SourceHolder, LangItem>(header) {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.catalogue_main_controller_card_item
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): SourceHolder {

        val view = inflater.inflate(layoutRes, parent, false)
        return SourceHolder(view, adapter as CatalogueMainAdapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: SourceHolder,
                                position: Int, payloads: List<Any?>?) {

        holder.bind(this)
    }

}