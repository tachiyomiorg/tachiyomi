package eu.kanade.tachiyomi.ui.extension

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.kanade.tachiyomi.R

/**
 * Item that contains the language header.
 *
 * @param code The lang code.
 */
data class ExtensionGroupItem(val installed: Boolean, val size: Int) : AbstractHeaderItem<ExtensionGroupHolder>() {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.extension_card_header
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<*>): ExtensionGroupHolder {
        return ExtensionGroupHolder(view, adapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: ExtensionGroupHolder,
                                position: Int, payloads: List<Any?>?) {

        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ExtensionGroupItem) {
            return installed == other.installed
        }
        return false
    }

    override fun hashCode(): Int {
        return installed.hashCode()
    }

}
