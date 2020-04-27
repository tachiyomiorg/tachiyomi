package eu.kanade.tachiyomi.ui.browse.extension

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.source.CatalogueSource

/**
 * Item that contains source information.
 *
 * @param source Instance of [CatalogueSource] containing source information.
 * @param header The header for this item.
 */
data class ExtensionItem(
    val extension: Extension,
    val header: ExtensionGroupItem? = null,
    val installStep: InstallStep? = null
) :
    AbstractSectionableItem<ExtensionHolder, ExtensionGroupItem>(header) {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.extension_card_item
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ExtensionHolder {
        return ExtensionHolder(view, adapter as ExtensionAdapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: ExtensionHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        if (payloads == null || payloads.isEmpty()) {
            holder.bind(this)
        } else {
            holder.bindButton(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return extension.pkgName == (other as ExtensionItem).extension.pkgName
    }

    override fun hashCode(): Int {
        return extension.pkgName.hashCode()
    }
}
