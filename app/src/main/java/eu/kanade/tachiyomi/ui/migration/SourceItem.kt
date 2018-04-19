package eu.kanade.tachiyomi.ui.migration

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source

/**
 * Item that contains source information.
 *
 * @param source Instance of [Source] containing source information.
 * @param header The header for this item.
 */
data class SourceItem(val source: Source, val header: SelectionHeader? = null) :
        AbstractSectionableItem<SourceHolder, SelectionHeader>(header) {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.catalogue_main_controller_card_item
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<*>): SourceHolder {
        return SourceHolder(view, adapter as SourceAdapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: SourceHolder,
                                position: Int, payloads: List<Any?>?) {

        holder.bind(this)
    }

}