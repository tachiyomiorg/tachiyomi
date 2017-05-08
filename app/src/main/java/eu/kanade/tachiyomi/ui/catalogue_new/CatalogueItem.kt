package eu.kanade.tachiyomi.ui.catalogue_new

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.inflate

class CatalogueItem(val source: Source) : AbstractFlexibleItem<CatalogueHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.catalogue_item
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueHolder {
        return CatalogueHolder(parent.inflate(layoutRes), adapter as CatalogueAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CatalogueHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(source)
    }

    override fun equals(other: Any?): Boolean {
        if (other is CatalogueItem) {
            return source.id == other.source.id
        }
        return false
    }

    override fun hashCode(): Int {
        return source.id.toInt()
    }

}