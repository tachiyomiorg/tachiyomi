package eu.kanade.tachiyomi.ui.browse.source

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource

/**
 * Item that contains source information.
 *
 * @param source Instance of [CatalogueSource] containing source information.
 * @param header The header for this item.
 */
data class SourceItem(
    val source: CatalogueSource,
    val header: LangItem? = null,
    val isPinned: Boolean = false
) :
    AbstractSectionableItem<SourceHolder, LangItem>(header) {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.source_main_controller_card_item
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): SourceHolder {
        return SourceHolder(view, adapter as SourceAdapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: SourceHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other is SourceItem) {
            return source.id == other.source.id && getHeader()?.code == other.getHeader()?.code
        }
        return false
    }

    override fun hashCode(): Int {
        return source.id.hashCode() + (getHeader()?.code?.hashCode() ?: 0).toInt()
    }
}
