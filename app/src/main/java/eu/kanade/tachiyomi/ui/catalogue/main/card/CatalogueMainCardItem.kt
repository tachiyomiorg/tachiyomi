package eu.kanade.tachiyomi.ui.catalogue.main.card

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.inflate

class CatalogueMainCardItem(val source: eu.kanade.tachiyomi.source.CatalogueSource) : eu.davidea.flexibleadapter.items.AbstractFlexibleItem<CatalogueMainCardHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.catalogue_main_controller_card_item
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueMainCardHolder {
        return CatalogueMainCardHolder(parent.inflate(layoutRes), adapter as CatalogueMainCardAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CatalogueMainCardHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(source)
    }

    override fun equals(other: Any?): Boolean {
        if (other is CatalogueMainCardItem) {
            return source.id == other.source.id
        }
        return false
    }

    override fun hashCode(): Int {
        return source.id.toInt()
    }

}